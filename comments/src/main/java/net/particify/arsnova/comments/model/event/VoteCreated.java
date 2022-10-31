package net.particify.arsnova.comments.model.event;

public class VoteCreated extends WebSocketEvent<VoteCreatedPayload> {
  public VoteCreated() {
    super(VoteCreated.class.getSimpleName());
  }

  public VoteCreated(VoteCreatedPayload p, String id) {
    super(VoteCreated.class.getSimpleName(), id);
    this.payload = p;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VoteCreated that = (VoteCreated) o;
    return this.getPayload().equals(that.getPayload());
  }
}
