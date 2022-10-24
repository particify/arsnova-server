package de.thm.arsnova.service;

import java.io.UnsupportedEncodingException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang.CharEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.SystemProperties;

@Service
public class EmailService {
  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  private final JavaMailSender mailSender;
  private final String mailSenderAddress;
  private final String mailSenderName;

  public EmailService(
      final JavaMailSender mailSender,
      final SystemProperties systemProperties) {
    this.mailSender = mailSender;
    this.mailSenderAddress = systemProperties.getMail().getSenderAddress();
    this.mailSenderName = systemProperties.getMail().getSenderName();
  }

  public void sendEmail(final String address, final String subject, final String body) {
    final MimeMessage msg = mailSender.createMimeMessage();
    final MimeMessageHelper helper = new MimeMessageHelper(msg, CharEncoding.UTF_8);
    try {
      msg.setHeader("Auto-Submitted", "auto-generated");
      helper.setFrom(mailSenderAddress, mailSenderName);
      helper.setTo(address);
      helper.setSubject(subject);
      helper.setText(body);

      logger.info("Sending mail \"{}\" from \"{}\" to \"{}\"", subject, msg.getFrom(), address);
      mailSender.send(msg);
    } catch (final MailException e) {
      logger.warn("Mail \"{}\" could not be sent.", subject, e);
      throw e;
    } catch (final MessagingException e) {
      logger.warn("Mail \"{}\" could not be sent because of MessagingException.", subject, e);
    } catch (final UnsupportedEncodingException e) {
      logger.error("Mail \"{}\" could not be sent because of the use of an unsupported encoding.", subject, e);
    }
  }
}
