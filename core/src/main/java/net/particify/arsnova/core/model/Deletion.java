package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Date;

import net.particify.arsnova.core.model.serialization.CouchDbTypeFieldConverter;
import net.particify.arsnova.core.model.serialization.View;

public class Deletion extends Entity {
  private Class<? extends Entity> deletedType;
  private Initiator initiator;
  private int count;

  public Deletion(final Class<? extends Entity> deletedType, final Initiator initiator, final int count) {
    this.creationTimestamp = new Date();
    this.deletedType = deletedType;
    this.initiator = initiator;
    this.count = count;
  }

  @JsonSerialize(converter = CouchDbTypeFieldConverter.class)
  @JsonView(View.Persistence.class)
  public Class<? extends Entity> getDeletedType() {
    return deletedType;
  }

  @JsonView(View.Persistence.class)
  public Initiator getInitiator() {
    return initiator;
  }

  @JsonView(View.Persistence.class)
  public int getCount() {
    return count;
  }

  public enum Initiator {
    USER,
    SYSTEM,
    CASCADE
  }
}
