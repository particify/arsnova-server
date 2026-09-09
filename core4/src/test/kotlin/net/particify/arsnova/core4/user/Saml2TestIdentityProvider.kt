/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.user

import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.xml.namespace.QName
import net.shibboleth.shared.xml.SerializeSupport
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.opensaml.core.config.InitializationService
import org.opensaml.core.xml.XMLObject
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
import org.opensaml.core.xml.schema.XSString
import org.opensaml.saml.saml2.core.Assertion
import org.opensaml.saml.saml2.core.Attribute
import org.opensaml.saml.saml2.core.AttributeStatement
import org.opensaml.saml.saml2.core.AttributeValue
import org.opensaml.saml.saml2.core.Audience
import org.opensaml.saml.saml2.core.AudienceRestriction
import org.opensaml.saml.saml2.core.AuthnContext
import org.opensaml.saml.saml2.core.AuthnContextClassRef
import org.opensaml.saml.saml2.core.AuthnStatement
import org.opensaml.saml.saml2.core.Conditions
import org.opensaml.saml.saml2.core.Issuer
import org.opensaml.saml.saml2.core.NameID
import org.opensaml.saml.saml2.core.Response
import org.opensaml.saml.saml2.core.Status
import org.opensaml.saml.saml2.core.StatusCode
import org.opensaml.saml.saml2.core.Subject
import org.opensaml.saml.saml2.core.SubjectConfirmation
import org.opensaml.saml.saml2.core.SubjectConfirmationData
import org.opensaml.security.x509.BasicX509Credential
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory
import org.opensaml.xmlsec.signature.Signature
import org.opensaml.xmlsec.signature.support.SignatureConstants
import org.opensaml.xmlsec.signature.support.Signer
import org.w3c.dom.Element

private const val KEY_SIZE = 2048
private const val CERTIFICATE_VALIDITY_DAYS = 365L
private const val ASSERTION_VALIDITY_MINUTES = 5L
private const val PEM_LINE_LENGTH = 64

/**
 * Fabricates signed SAML responses so the converter can be driven through the real filter chain
 * without an identity provider to talk to. Spring Security's own SAML test fixtures are not
 * published, so the assertion is assembled with OpenSAML directly.
 *
 * The signing key is generated per test run and written to a temporary directory, which keeps key
 * material out of the repository. The same credential backs the relying party's own signing
 * configuration, which is mandatory but never exercised here: no request is ever sent to the
 * identity provider.
 */
class Saml2TestIdentityProvider {
  private val credential: BasicX509Credential
  private val directory: Path = Files.createTempDirectory("core4-saml2-test")

  val privateKeyLocation: String
  val certificateLocation: String
  val metadataLocation: String

  init {
    InitializationService.initialize()
    val keyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(KEY_SIZE) }.generateKeyPair()
    val certificate = selfSign(keyPair.private, keyPair.public)
    credential = BasicX509Credential(certificate, keyPair.private)
    privateKeyLocation = writePem("idp.key", "PRIVATE KEY", keyPair.private.encoded)
    certificateLocation = writePem("idp.crt", "CERTIFICATE", certificate.encoded)
    metadataLocation = writeMetadata(certificate)
    directory.toFile().deleteOnExit()
  }

  /** Encodes a signed response for the given user, ready to be posted to the assertion consumer. */
  fun encodedResponse(registrationId: UUID, user: Saml2TestUser): String {
    val assertion = assertion(registrationId, user)
    val response = response(registrationId, assertion)
    val element = marshall(response)
    Signer.signObject(checkNotNull(assertion.signature))
    return Base64.getEncoder().encodeToString(SerializeSupport.nodeToString(element).toByteArray())
  }

  private fun response(registrationId: UUID, assertion: Assertion): Response {
    val response = buildSamlObject<Response>(Response.DEFAULT_ELEMENT_NAME)
    response.id = "_${UUID.randomUUID()}"
    response.issueInstant = Instant.now()
    response.destination = acsLocation(registrationId)
    response.issuer = issuer()
    response.status = status()
    response.assertions.add(assertion)
    return response
  }

  private fun status(): Status {
    val statusCode = buildSamlObject<StatusCode>(StatusCode.DEFAULT_ELEMENT_NAME)
    statusCode.value = StatusCode.SUCCESS
    val status = buildSamlObject<Status>(Status.DEFAULT_ELEMENT_NAME)
    status.statusCode = statusCode
    return status
  }

  private fun assertion(registrationId: UUID, user: Saml2TestUser): Assertion {
    val now = Instant.now()
    val assertion = buildSamlObject<Assertion>(Assertion.DEFAULT_ELEMENT_NAME)
    assertion.id = "_${UUID.randomUUID()}"
    assertion.issueInstant = now
    assertion.issuer = issuer()
    assertion.subject = subject(registrationId, user, now)
    assertion.conditions = conditions(registrationId, now)
    assertion.authnStatements.add(authnStatement(now))
    assertion.attributeStatements.add(attributeStatement(user))
    assertion.signature = signature()
    return assertion
  }

  private fun subject(registrationId: UUID, user: Saml2TestUser, now: Instant): Subject {
    val nameId = buildSamlObject<NameID>(NameID.DEFAULT_ELEMENT_NAME)
    nameId.format = NameID.UNSPECIFIED
    nameId.value = user.subjectId
    val confirmationData =
        buildSamlObject<SubjectConfirmationData>(SubjectConfirmationData.DEFAULT_ELEMENT_NAME)
    confirmationData.notOnOrAfter = now.plus(ASSERTION_VALIDITY_MINUTES, ChronoUnit.MINUTES)
    confirmationData.recipient = acsLocation(registrationId)
    val confirmation =
        buildSamlObject<SubjectConfirmation>(SubjectConfirmation.DEFAULT_ELEMENT_NAME)
    confirmation.method = SubjectConfirmation.METHOD_BEARER
    confirmation.subjectConfirmationData = confirmationData
    val subject = buildSamlObject<Subject>(Subject.DEFAULT_ELEMENT_NAME)
    subject.nameID = nameId
    subject.subjectConfirmations.add(confirmation)
    return subject
  }

  private fun conditions(registrationId: UUID, now: Instant): Conditions {
    val audience = buildSamlObject<Audience>(Audience.DEFAULT_ELEMENT_NAME)
    audience.uri = spEntityId(registrationId)
    val restriction = buildSamlObject<AudienceRestriction>(AudienceRestriction.DEFAULT_ELEMENT_NAME)
    restriction.audiences.add(audience)
    val conditions = buildSamlObject<Conditions>(Conditions.DEFAULT_ELEMENT_NAME)
    conditions.notBefore = now.minus(ASSERTION_VALIDITY_MINUTES, ChronoUnit.MINUTES)
    conditions.notOnOrAfter = now.plus(ASSERTION_VALIDITY_MINUTES, ChronoUnit.MINUTES)
    conditions.conditions.add(restriction)
    return conditions
  }

  private fun authnStatement(now: Instant): AuthnStatement {
    val classRef = buildSamlObject<AuthnContextClassRef>(AuthnContextClassRef.DEFAULT_ELEMENT_NAME)
    classRef.uri = AuthnContext.UNSPECIFIED_AUTHN_CTX
    val context = buildSamlObject<AuthnContext>(AuthnContext.DEFAULT_ELEMENT_NAME)
    context.authnContextClassRef = classRef
    val statement = buildSamlObject<AuthnStatement>(AuthnStatement.DEFAULT_ELEMENT_NAME)
    statement.authnInstant = now
    statement.sessionIndex = UUID.randomUUID().toString()
    statement.authnContext = context
    return statement
  }

  private fun attributeStatement(user: Saml2TestUser): AttributeStatement {
    val statement = buildSamlObject<AttributeStatement>(AttributeStatement.DEFAULT_ELEMENT_NAME)
    for ((name, value) in user.attributes) {
      statement.attributes.add(attribute(name, value))
    }
    return statement
  }

  private fun attribute(name: String, value: String): Attribute {
    val attribute = buildSamlObject<Attribute>(Attribute.DEFAULT_ELEMENT_NAME)
    attribute.name = name
    attribute.nameFormat = Attribute.URI_REFERENCE
    attribute.attributeValues.add(attributeValue(value))
    return attribute
  }

  private fun attributeValue(value: String): XSString {
    val builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory()
    val builder = checkNotNull(builderFactory.getBuilder(XSString.TYPE_NAME))
    val attributeValue =
        builder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME) as XSString
    attributeValue.value = value
    return attributeValue
  }

  private fun issuer(): Issuer {
    val issuer = buildSamlObject<Issuer>(Issuer.DEFAULT_ELEMENT_NAME)
    issuer.value = SAML_IDP_ENTITY_ID
    return issuer
  }

  private fun signature(): Signature {
    val signature = buildSamlObject<Signature>(Signature.DEFAULT_ELEMENT_NAME)
    signature.signingCredential = credential
    signature.signatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256
    signature.canonicalizationAlgorithm = SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS
    val keyInfoFactory = X509KeyInfoGeneratorFactory()
    keyInfoFactory.setEmitEntityCertificate(true)
    signature.keyInfo = keyInfoFactory.newInstance().generate(credential)
    return signature
  }

  private fun marshall(response: Response): Element {
    val marshallerFactory = XMLObjectProviderRegistrySupport.getMarshallerFactory()
    val marshaller = checkNotNull(marshallerFactory.getMarshaller(response))
    return marshaller.marshall(response)
  }

  private fun selfSign(privateKey: PrivateKey, publicKey: PublicKey): X509Certificate {
    val now = Instant.now()
    val name = X500Name("CN=core4-saml2-test-idp")
    val builder =
        JcaX509v3CertificateBuilder(
            name,
            BigInteger.ONE,
            Date.from(now.minus(1, ChronoUnit.DAYS)),
            Date.from(now.plus(CERTIFICATE_VALIDITY_DAYS, ChronoUnit.DAYS)),
            name,
            publicKey)
    val signer = JcaContentSignerBuilder("SHA256WithRSA").build(privateKey)
    return JcaX509CertificateConverter().getCertificate(builder.build(signer))
  }

  private fun writePem(fileName: String, label: String, der: ByteArray): String {
    val body = Base64.getMimeEncoder(PEM_LINE_LENGTH, "\n".toByteArray()).encodeToString(der)
    val pem = "-----BEGIN $label-----\n$body\n-----END $label-----\n"
    return writeFile(fileName, pem)
  }

  private fun writeMetadata(certificate: X509Certificate): String {
    val encoded = Base64.getEncoder().encodeToString(certificate.encoded)
    val metadata =
        """
        <md:EntityDescriptor xmlns:md="urn:oasis:names:tc:SAML:2.0:metadata"
            entityID="$SAML_IDP_ENTITY_ID">
          <md:IDPSSODescriptor WantAuthnRequestsSigned="false"
              protocolSupportEnumeration="urn:oasis:names:tc:SAML:2.0:protocol">
            <md:KeyDescriptor use="signing">
              <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                <ds:X509Data><ds:X509Certificate>$encoded</ds:X509Certificate></ds:X509Data>
              </ds:KeyInfo>
            </md:KeyDescriptor>
            <md:SingleSignOnService
                Binding="urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST"
                Location="$SAML_IDP_ENTITY_ID/sso"/>
          </md:IDPSSODescriptor>
        </md:EntityDescriptor>
        """
            .trimIndent()
    return writeFile("idp-metadata.xml", metadata)
  }

  private fun writeFile(fileName: String, content: String): String {
    val path = directory.resolve(fileName)
    Files.writeString(path, content)
    path.toFile().deleteOnExit()
    return "file:$path"
  }
}

@Suppress("UNCHECKED_CAST")
private fun <T : XMLObject> buildSamlObject(elementName: QName): T {
  val builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory()
  val builder = checkNotNull(builderFactory.getBuilder(elementName)) { "No builder: $elementName" }
  return builder.buildObject(elementName) as T
}
