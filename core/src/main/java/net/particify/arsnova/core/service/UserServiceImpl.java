/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2019 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.particify.arsnova.core.service;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.ektorp.DbAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.client.WebClient;

import net.particify.arsnova.core.config.properties.AuthenticationProviderProperties;
import net.particify.arsnova.core.config.properties.SecurityProperties;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.Deletion.Initiator;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.persistence.UserRepository;
import net.particify.arsnova.core.security.PasswordUtils;
import net.particify.arsnova.core.security.User;
import net.particify.arsnova.core.service.exceptions.UserAlreadyExistsException;
import net.particify.arsnova.core.web.exceptions.BadRequestException;
import net.particify.arsnova.core.web.exceptions.NotFoundException;

/**
 * Performs all user related operations.
 */
@Service
@Primary
public class UserServiceImpl extends DefaultEntityServiceImpl<UserProfile> implements UserService {
  private static final int MAIL_RESEND_TRY_RESET_DELAY_MS = 30 * 1000;

  private static final int MAIL_RESEND_BAN_RESET_DELAY_MS = 2 * 60 * 1000;

  private static final int REPEATED_PASSWORD_RESET_DELAY_MS = 3 * 60 * 1000;

  private static final int PASSWORD_RESET_KEY_DURABILITY_MS = 2 * 60 * 60 * 1000;

  private static final long ACTIVATION_KEY_CHECK_INTERVAL_MS = 30 * 60 * 1000L;
  private static final long ACTIVATION_KEY_DURABILITY_MS = 5 * 24 * 60 * 60 * 1000L;
  private static final long INACTIVE_USERS_CHECK_INTERVAL_MS = 60 * 60 * 1000L;

  private static final int VERIFICATION_CODE_LENGTH = 6;
  private static final int MAX_VERIFICATION_CODE_ATTEMPTS = 10;

  /* Password constraint statics */
  private static final int PASSWORD_CONSTRAINT_MIN_LENGTH = 8;
  // When a password is extra long, it contributes to the security level
  private static final int PASSWORD_CONSTRAINT_EXTRA_LENGTH = 20;
  private static final Pattern PASSWORD_CONSTRAINT_NUMBERS_PATTERN = Pattern.compile("\\d");
  private static final Pattern PASSWORD_CONSTRAINT_LOWER_CASE_PATTERN = Pattern.compile("[\\p{Ll}]");
  private static final Pattern PASSWORD_CONSTRAINT_UPPER_CASE_PATTERN = Pattern.compile("[\\p{Lu}]");
  private static final Pattern PASSWORD_CONSTRAINT_SPECIAL_CHAR_PATTERN = Pattern.compile("[\\p{P}\\p{S}\\p{Z}]");

  private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

  private UserRepository userRepository;
  private PasswordUtils passwordUtils;
  private EmailService emailService;

  private SecurityProperties securityProperties;
  private AuthenticationProviderProperties.Registered registeredProperties;

  private String rootUrl;
  private Duration userInactivityPeriod;
  private int userInactivityLimit;

  private Pattern mailPattern;
  private ConcurrentHashMap<String, Byte> resentMailCount;
  private Set<String> resendMailBans;
  private WebClient authzWebClient;

  {
    resentMailCount = new ConcurrentHashMap<>();
    resendMailBans = Collections.synchronizedSet(new HashSet<String>());
  }

  public UserServiceImpl(
      final UserRepository repository,
      final DeletionRepository deletionRepository,
      final SystemProperties systemProperties,
      final SecurityProperties securityProperties,
      final AuthenticationProviderProperties authenticationProviderProperties,
      final EmailService emailService,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator,
      final PasswordUtils passwordUtils) {
    super(UserProfile.class, repository, deletionRepository, jackson2HttpMessageConverter.getObjectMapper(), validator);
    this.userRepository = repository;
    this.securityProperties = securityProperties;
    this.registeredProperties = authenticationProviderProperties.getRegistered();
    this.passwordUtils = passwordUtils;
    this.emailService = emailService;
    this.rootUrl = systemProperties.getRootUrl();
    final String authzUrl = systemProperties.getAuthzServiceUrl();
    this.userInactivityPeriod = systemProperties.getAutoDeletionThresholds().getUserInactivityPeriod();
    this.userInactivityLimit = systemProperties.getAutoDeletionThresholds().getUserInactivityLimit();
    this.authzWebClient = WebClient.create(authzUrl);
  }

  @Scheduled(fixedDelay = MAIL_RESEND_TRY_RESET_DELAY_MS)
  public void resetResendingActivationTries() {
    if (!resentMailCount.isEmpty()) {
      logger.debug("Resetting counters for resent activation mails.");
      resentMailCount.clear();
    }
  }

  @Scheduled(fixedDelay = MAIL_RESEND_BAN_RESET_DELAY_MS)
  public void resetResendingActivationBan() {
    if (!resendMailBans.isEmpty()) {
      logger.info("Clearing temporary bans for resent activation mails ({}).", resendMailBans.size());
      resendMailBans.clear();
    }
  }

  @Scheduled(fixedDelay = ACTIVATION_KEY_CHECK_INTERVAL_MS)
  public void deleteNonActivatedUsers() {
    logger.debug("Deleting non-activated user accounts.");
    final long unixTime = System.currentTimeMillis();
    final long creationBefore = unixTime - ACTIVATION_KEY_DURABILITY_MS;
    userRepository.deleteNonActivatedUsers(creationBefore);
  }

  @Scheduled(fixedDelay = INACTIVE_USERS_CHECK_INTERVAL_MS)
  public void deleteInactiveUsers() {
    if (userInactivityPeriod.isZero()) {
      logger.trace("Skipping deletion of inactive user accounts.");
      return;
    }
    final Instant lastActiveBefore = Instant.now().minus(userInactivityPeriod);
    final String lastActiveBeforeParam = lastActiveBefore.toString();
    logger.debug("Retrieving IDs of inactive (since {}) user accounts.", lastActiveBeforeParam);
    final List<String> userIds = authzWebClient
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/roomaccess/inactive-user-ids")
            .queryParam("lastActiveBefore", lastActiveBeforeParam)
            .build())
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
        .block();
    if (userIds.size() > userInactivityLimit) {
      logger.warn("Number of inactive users ({}) is over the threshold of {}. Accounts will not be deleted.",
          userIds.size(), userInactivityLimit);
      return;
    }
    logger.info("Deleting {} inactive (since {}) user accounts.", userIds.size(), lastActiveBeforeParam);
    final AtomicInteger count = new AtomicInteger();
    final Collection<List<String>> groupedUserIds = userIds.stream()
        .collect(Collectors.groupingBy(uid -> count.getAndIncrement() / 20))
        .values();
    for (final List<String> userIdList : groupedUserIds) {
      final List<UserProfile> userProfiles = get(userIdList);
      delete(userProfiles, Initiator.SYSTEM);
    }
  }

  @Override
  protected void prepareCreate(final UserProfile userProfile) {
    if (userProfile.getAuthProvider() == UserProfile.AuthProvider.ARSNOVA) {
      userProfile.setLoginId(userProfile.getLoginId().toLowerCase());
    }
    if (null != userRepository.findByAuthProviderAndLoginId(
        userProfile.getAuthProvider(), userProfile.getLoginId())) {
      logger.info("User registration failed. {} already exists.", userProfile.getLoginId());
      throw new UserAlreadyExistsException();
    }
  }

  @Override
  public boolean isAdmin(final String loginId, final UserProfile.AuthProvider authProvider) {
    return securityProperties.getAdminAccounts().contains(
        new SecurityProperties.AdminAccount(loginId, authProvider));
  }

  private boolean isBannedFromSendingActivationMail(final String addr) {
    return resendMailBans.contains(addr);
  }

  private void increaseSentMailCount(final String addr) {
    Byte tries = resentMailCount.get(addr);
    if (null == tries) {
      tries = 0;
    }
    if (tries < securityProperties.getResendMailLimit()) {
      resentMailCount.put(addr, ++tries);
      if (securityProperties.getResendMailLimit() == tries) {
        logger.info("Temporarily banned {} from resending activation"
            + " mails in due to too many resent activation mails.", addr);
        resendMailBans.add(addr);
      }
    }
  }

  @Override
  public User loadUser(final UserProfile.AuthProvider authProvider, final String loginId,
      final Collection<GrantedAuthority> grantedAuthorities, final boolean autoCreate)
      throws UsernameNotFoundException {
    logger.debug("Load user: LoginId: {}, AuthProvider: {}", loginId, authProvider);
    UserProfile userProfile = userRepository.findByAuthProviderAndLoginId(authProvider, loginId);
    if (userProfile == null) {
      if (autoCreate) {
        userProfile = new UserProfile(authProvider, loginId);
        create(userProfile);
      } else {
        throw new UsernameNotFoundException("User does not exist.");
      }
    }

    return new User(userProfile, grantedAuthorities);
  }

  @Override
  public User loadUser(final String userId, final Collection<GrantedAuthority> grantedAuthorities)
      throws UsernameNotFoundException {
    logger.debug("Load user: UserId: {}", userId);
    final UserProfile userProfile = get(userId, true);
    if (userProfile == null) {
      throw new UsernameNotFoundException("User does not exist.");
    }

    return new User(userProfile, grantedAuthorities);
  }

  @Override
  public UserProfile getByAuthProviderAndLoginId(final UserProfile.AuthProvider authProvider, final String loginId) {
    return userRepository.findByAuthProviderAndLoginId(authProvider, loginId);
  }

  @Override
  public List<UserProfile> getByLoginId(final String loginId) {
    return userRepository.findByLoginId(loginId);
  }

  @Override
  public UserProfile getByUsername(final String username) {
    return userRepository.findByAuthProviderAndLoginId(UserProfile.AuthProvider.ARSNOVA, username.toLowerCase());
  }

  @Override
  public UserProfile create(final String username, final String password) {
    final String lcUsername = username.toLowerCase();

    if (null == mailPattern) {
      parseMailAddressPattern();
    }

    if (null == mailPattern || !mailPattern.matcher(lcUsername).matches()) {
      logger.info("User registration failed. {} does not match pattern.", lcUsername);

      return null;
    }

    if (password.length() < PASSWORD_CONSTRAINT_MIN_LENGTH) {
      logger.debug("User registration failed. Password is shorter than 8 characters.");

      return null;
    }

    if (getPasswordStrength(password) < securityProperties.getPasswordStrictnessLevel()) {
      logger.debug("User registration failed. Password is not strong enough.");

      return null;
    }

    final UserProfile userProfile = new UserProfile();
    final UserProfile.Account account = new UserProfile.Account();
    userProfile.setAccount(account);
    userProfile.setAuthProvider(UserProfile.AuthProvider.ARSNOVA);
    userProfile.setLoginId(lcUsername);
    account.setPassword(encodePassword(password));
    account.setActivationKey(generateVerificationCode());
    userProfile.setCreationTimestamp(new Date());

    final UserProfile result = create(userProfile);
    logger.debug("Activation key for user '{}': {}",
        userProfile.getLoginId(),
        account.getActivationKey());
    sendActivationEmail(result);

    return result;
  }

  @Override
  public UserProfile createAnonymizedGuestUser() {
    final UserProfile userProfile = new UserProfile(UserProfile.AuthProvider.ANONYMIZED, generateGuestId());
    create(userProfile);
    return userProfile;
  }

  private int getPasswordStrength(final String password) {
    int passwordStrength = 0;

    if (password.length() >= PASSWORD_CONSTRAINT_EXTRA_LENGTH) {
      passwordStrength++;
    }

    final Matcher numbersMatcher = PASSWORD_CONSTRAINT_NUMBERS_PATTERN.matcher(password);
    if (numbersMatcher.find()) {
      passwordStrength++;
    }

    final Matcher lowerCaseMatcher = PASSWORD_CONSTRAINT_LOWER_CASE_PATTERN.matcher(password);
    if (lowerCaseMatcher.find()) {
      passwordStrength++;
    }

    final Matcher upperCaseMatcher = PASSWORD_CONSTRAINT_UPPER_CASE_PATTERN.matcher(password);
    if (upperCaseMatcher.find()) {
      passwordStrength++;
    }

    final Matcher specialCharMatcher = PASSWORD_CONSTRAINT_SPECIAL_CHAR_PATTERN.matcher(password);
    if (specialCharMatcher.find()) {
      passwordStrength++;
    }

    return passwordStrength;
  }

  private String encodePassword(final String password) {
    return passwordUtils.encode(password);
  }

  private void sendActivationEmail(final UserProfile userProfile) {
    final String activationKey = userProfile.getAccount().getActivationKey();

    sendEmail(userProfile,
        MessageFormat.format(
            registeredProperties.getRegistrationMailSubject(), activationKey),
        MessageFormat.format(
            registeredProperties.getRegistrationMailBody(), activationKey, rootUrl));
  }

  private void parseMailAddressPattern() {
    /* TODO: Add Unicode support */

    if (!registeredProperties.getAllowedEmailDomains().isEmpty()) {
      final List<String> patterns = new ArrayList<>();
      if (registeredProperties.getAllowedEmailDomains().contains("*")) {
        patterns.add("([a-z0-9-]+\\.)+[a-z0-9-]+");
      } else {
        final Pattern patternPattern = Pattern.compile("[a-z0-9.*-]+", Pattern.CASE_INSENSITIVE);
        for (final String patternStr : registeredProperties.getAllowedEmailDomains()) {
          if (patternPattern.matcher(patternStr).matches()) {
            patterns.add(
                patternStr.replaceAll("[.]", "[.]").replaceAll("[*]", "[a-z0-9-]+?"));
          }
        }
      }

      mailPattern = Pattern.compile(
          "[a-z0-9._-]+?@(" + patterns.stream().collect(Collectors.joining("|")) + ")",
          Pattern.CASE_INSENSITIVE);
      logger.info("Allowed e-mail addresses (pattern) for registration: '{}'.", mailPattern.pattern());
    }
  }

  @Override
  public UserProfile resetActivation(final String id, final String clientAddress) {
    if (isBannedFromSendingActivationMail(clientAddress)) {
      return null;
    }
    final UserProfile userProfile = get(id, true);
    if (userProfile == null || userProfile.getAccount().isActivated()) {
      logger.info("Reset of account activation failed. No key exists for user {}.", id);
      // No mail is sent, but we can use the same counter here.
      increaseSentMailCount(clientAddress);

      throw new NotFoundException();
    }
    increaseSentMailCount(clientAddress);
    sendActivationEmail(userProfile);

    return userProfile;
  }

  @Override
  public boolean activateAccount(final String id, final String key, final String clientAddress) {
    final UserProfile userProfile = get(id, true);
    if (userProfile == null || !key.equals(userProfile.getAccount().getActivationKey())) {
      if (userProfile != null) {
        final int failedVerifications = userProfile.getAccount().getFailedVerifications() + 1;
        userProfile.getAccount().setFailedVerifications(failedVerifications);
        if (failedVerifications >= MAX_VERIFICATION_CODE_ATTEMPTS) {
          logger.info("Resetting activation verification code because of too many failed attempts for {}.",
              userProfile.getLoginId());
          userProfile.getAccount().setActivationKey(generateVerificationCode());
          userProfile.getAccount().setFailedVerifications(0);
        }
        update(userProfile);
      }

      return false;
    }

    userProfile.getAccount().setActivationKey(null);
    userProfile.getAccount().setFailedVerifications(0);
    update(userProfile);

    return true;
  }

  @Override
  public void activateAccount(final String id) {
    final UserProfile userProfile = get(id, true);
    userProfile.getAccount().setActivationKey(null);
    userProfile.getAccount().setFailedVerifications(0);
    update(userProfile);
  }

  @Override
  public void initiatePasswordReset(final String id) {
    final UserProfile userProfile = get(id);
    if (null == userProfile) {
      logger.info("Password reset failed. User does not exist.");

      throw new NotFoundException();
    }
    final UserProfile.Account account = userProfile.getAccount();
    // checks if a password reset process in in progress
    if ((account.getPasswordResetTime() != null)
        && (System.currentTimeMillis()
            < account.getPasswordResetTime().getTime() + REPEATED_PASSWORD_RESET_DELAY_MS)) {

      logger.info("Password reset failed. The reset delay for User {} is still active.", userProfile.getLoginId());

      throw new BadRequestException();
    }

    account.setPasswordResetKey(generateVerificationCode());
    account.setPasswordResetTime(new Date());
    account.setFailedVerifications(0);
    try {
      update(userProfile);
      logger.debug("Password reset key for user '{}': {}",
          userProfile.getLoginId(),
          account.getPasswordResetKey());
    } catch (final DbAccessException e) {
      logger.error("Password reset failed. {} could not be updated.", userProfile.getLoginId());
      throw e;
    }

    sendEmail(userProfile,
        MessageFormat.format(
            registeredProperties.getResetPasswordMailSubject(), account.getPasswordResetKey()),
        MessageFormat.format(
            registeredProperties.getResetPasswordMailBody(), account.getPasswordResetKey(), rootUrl));
  }

  @Override
  public boolean resetPassword(final String id, final String key, final String password) {
    final UserProfile userProfile = get(id);
    if (userProfile == null || userProfile.getAccount() == null) {
      return false;
    }
    final UserProfile.Account account = userProfile.getAccount();
    if (null == key || "".equals(key) || !key.equals(account.getPasswordResetKey())) {
      logger.info("Password reset failed. Invalid key provided for User {}.", userProfile.getLoginId());

      if (key != null && !key.equals(account.getPasswordResetKey())) {
        final int failedVerifications = userProfile.getAccount().getFailedVerifications() + 1;
        userProfile.getAccount().setFailedVerifications(failedVerifications);
        if (failedVerifications >= MAX_VERIFICATION_CODE_ATTEMPTS) {
          logger.info(
              "Resetting password reset verification code because of too many failed attempts for {}.",
              userProfile.getLoginId());
          userProfile.getAccount().setPasswordResetKey(null);
          account.setPasswordResetTime(new Date(0));
          account.setFailedVerifications(0);
        }
        update(userProfile);
      }

      return false;
    }
    if (System.currentTimeMillis() > account.getPasswordResetTime().getTime() + PASSWORD_RESET_KEY_DURABILITY_MS) {
      logger.info("Password reset failed. Key provided for User {} is no longer valid.", userProfile.getLoginId());

      account.setPasswordResetKey(null);
      account.setPasswordResetTime(new Date(0));
      account.setFailedVerifications(0);
      update(userProfile);

      return false;
    }

    account.setPassword(encodePassword(password));
    account.setPasswordResetKey(null);
    try {
      update(userProfile);
    } catch (final DbAccessException e) {
      logger.error("Password reset failed. {} could not be updated.", userProfile.getLoginId());
      throw e;
    }

    return true;
  }

  public String generateGuestId() {
    return passwordUtils.generateKey();
  }

  private void sendEmail(final UserProfile userProfile, final String subject, final String body) {
    emailService.sendEmail(userProfile.getLoginId(), subject, body);
  }

  private String generateVerificationCode() {
    return passwordUtils.generateFixedLengthNumericCode(VERIFICATION_CODE_LENGTH);
  }
}
