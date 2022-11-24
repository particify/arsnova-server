package net.particify.arsnova.comments.model.command;

public class PatchVote extends WebSocketCommand<PatchVotePayload> {
  public PatchVote() {
    super(PatchVote.class.getSimpleName());
  }

  public PatchVote(PatchVotePayload p) {
    super(PatchVote.class.getSimpleName());
    this.payload = p;
  }
}