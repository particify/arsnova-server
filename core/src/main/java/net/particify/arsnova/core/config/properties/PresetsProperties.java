package net.particify.arsnova.core.config.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(PresetsProperties.PREFIX)
public class PresetsProperties {
  public static final String PREFIX = "presets";

  private List<String> templateLicenses;

  public List<String> getTemplateLicenses() {
    return templateLicenses;
  }

  public void setTemplateLicenses(final List<String> templateLicenses) {
    this.templateLicenses = templateLicenses;
  }
}
