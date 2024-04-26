package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.validation.LanguageIso639;
import net.particify.arsnova.core.validation.TemplateLicense;

public class ContentGroupTemplate extends Entity {
  @NotBlank
  @Size(max = 50)
  private String name;

  @NotNull
  private ContentGroup.GroupType groupType = ContentGroup.GroupType.MIXED;

  @Size(max = 250)
  private String description;

  @LanguageIso639
  private String language;

  @TemplateLicense
  private String license;

  @Size(max = 50)
  private String attribution;

  private boolean aiGenerated;

  private List<String> templateIds;
  private List<String> tagIds;
  private List<TemplateTag> tags;

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
  public ContentGroup.GroupType getGroupType() {
    return groupType;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setGroupType(final ContentGroup.GroupType groupType) {
    this.groupType = groupType;
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
  public String getAttribution() {
    return attribution;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setAttribution(final String attribution) {
    this.attribution = attribution;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public boolean isAiGenerated() {
    return aiGenerated;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setAiGenerated(final boolean aiGenerated) {
    this.aiGenerated = aiGenerated;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public List<String> getTemplateIds() {
    return templateIds;
  }

  @JsonView(View.Persistence.class)
  public void setTemplateIds(final List<String> templateIds) {
    this.templateIds = templateIds;
  }

  @JsonView(View.Persistence.class)
  public List<String> getTagIds() {
    return tagIds;
  }

  @JsonView(View.Persistence.class)
  public void setTagIds(final List<String> tagIds) {
    this.tagIds = tagIds;
  }

  @JsonView(View.Public.class)
  public List<TemplateTag> getTags() {
    return tags;
  }

  @JsonView(View.Public.class)
  public void setTags(final List<TemplateTag> tags) {
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
