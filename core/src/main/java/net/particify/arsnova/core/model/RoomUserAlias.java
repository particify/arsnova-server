package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotEmpty;

import net.particify.arsnova.core.model.serialization.View;

public class RoomUserAlias extends Entity implements RoomIdAware {
  @NotEmpty
  private String roomId;

  @NotEmpty
  private String userId;

  private String alias;
  /** The seed is used to generate an alias if no {@link alias} is set. */
  private int seed;

  @JsonView(View.Persistence.class)
  public String getRoomId() {
    return roomId;
  }

  @JsonView(View.Persistence.class)
  public void setRoomId(final String roomId) {
    this.roomId = roomId;
  }

  @JsonView(View.Persistence.class)
  public String getUserId() {
    return userId;
  }

  @JsonView(View.Persistence.class)
  public void setUserId(final String userId) {
    this.userId = userId;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public String getAlias() {
    return alias;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setAlias(final String alias) {
    this.alias = alias;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public int getSeed() {
    return seed;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setSeed(final int seed) {
    this.seed = seed;
  }
}
