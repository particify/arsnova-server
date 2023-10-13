package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;

import net.particify.arsnova.core.model.serialization.View;

public class TemplateTag extends Entity {
  @NotBlank
  private String name;

  private boolean verified;

  private String language;

  public TemplateTag() {

  }

  public TemplateTag(final String name, final String language) {
    this.name = name;
    this.language = language;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getName() {
    return name;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setName(final String name) {
    this.name = name;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isVerified() {
    return verified;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setVerified(final boolean verified) {
    this.verified = verified;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getLanguage() {
    return language;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setLanguage(final String language) {
    this.language = language;
  }
}
