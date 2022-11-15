package net.particify.arsnova.core.security;

import java.security.SecureRandom;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.keygen.BytesKeyGenerator;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtils {
  private static final int BCRYPT_STRENGTH = 12;

  private SecureRandom secureRandom = new SecureRandom();
  private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
  private BytesKeyGenerator keygen = KeyGenerators.secureRandom(16);

  public String encode(final String password) {
    return encoder.encode(password);
  }

  public boolean matches(final String rawPassword, final String encodedPassword) {
    return encoder.matches(rawPassword, encodedPassword);
  }

  public String generateKey() {
    return new String(Hex.encode(keygen.generateKey()));
  }

  public String generateFixedLengthNumericCode(final int length) {
    final long code = secureRandom.nextLong(Long.MAX_VALUE);
    // Reverse the string (the first digits have less randomness) and prepend
    // zeros if code is shorter than length.
    return new StringBuilder(String.format("%0" + length + "d", code))
        .reverse().substring(0, length);
  }
}
