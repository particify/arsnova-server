package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;

public class ContentGroupTemplate extends Entity {
  @NotBlank
  @Size(max = 25)
  private String name;

  @Size(max = 250)
  private String description;

  private String language;

  @NotBlank
  private String license;

  private List<String> templateIds;
  private List<String> tags;

  private String creatorId;

  @JsonView({View.Persistence.class, View.Public.class})
  public String getName() {
    return name;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setName(final String name) {
    this.name = name;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getDescription() {
    return description;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setDescription(final String description) {
    this.description = description;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getLanguage() {
    return language;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setLanguage(final String language) {
    this.language = language;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getLicense() {
    return license;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setLicense(final String license) {
    this.license = license;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public List<String> getTemplateIds() {
    return templateIds;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setTemplateIds(final List<String> templateIds) {
    this.templateIds = templateIds;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public List<String> getTags() {
    return tags;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setTags(final List<String> tags) {
    this.tags = tags;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getCreatorId() {
    return creatorId;
  }

  @JsonView(View.Persistence.class)
  public void setCreatorId(final String creatorId) {
    this.creatorId = creatorId;
  }
}
