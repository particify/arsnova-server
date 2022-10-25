package net.particify.arsnova.comments.model.command;

public class PatchVote extends WebSocketCommand<PatchVotePayload> {
  public PatchVote() {
    super(PatchVote.class.getSimpleName());
  }

  public PatchVote(PatchVotePayload p) {
    super(PatchVote.class.getSimpleName());
    this.payload = p;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PatchVote that = (PatchVote) o;
    return this.getPayload().equals(that.getPayload());
  }
}