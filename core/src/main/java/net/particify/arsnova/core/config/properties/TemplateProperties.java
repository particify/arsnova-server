package net.particify.arsnova.core.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(TemplateProperties.PREFIX)
public class TemplateProperties {
  public static final String PREFIX = "templates";

  private String roomInvitationUrl;
  private String roomInvitationMailSubject;
  private String roomInvitationMailBody;

  public String getRoomInvitationUrl() {
    return roomInvitationUrl;
  }

  public void setRoomInvitationUrl(final String roomInvitationUrl) {
    this.roomInvitationUrl = roomInvitationUrl;
  }

  public String getRoomInvitationMailSubject() {
    return roomInvitationMailSubject;
  }

  public void setRoomInvitationMailSubject(final String roomInvitationMailSubject) {
    this.roomInvitationMailSubject = roomInvitationMailSubject;
  }

  public String getRoomInvitationMailBody() {
    return roomInvitationMailBody;
  }

  public void setRoomInvitationMailBody(final String roomInvitationMailBody) {
    this.roomInvitationMailBody = roomInvitationMailBody;
  }
}
