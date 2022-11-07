package net.particify.arsnova.comments.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = SecurityProperties.PREFIX)
public class SecurityProperties {
  public static final String PREFIX = "security";

  public static class Jwt {
    private String secret;

    public Jwt(String secret) {
      this.secret = secret;
    }

    public String getSecret() {
      return secret;
    }
  }

  private Jwt jwt;

  public SecurityProperties(Jwt jwt) {
    this.jwt = jwt;
  }

  public Jwt getJwt() {
    return jwt;
  }
}
