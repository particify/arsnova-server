package net.particify.arsnova.comments.model.command;

public class ResetVote extends WebSocketCommand<VotePayload> {
  public ResetVote() {
    super(ResetVote.class.getSimpleName());
  }

  public ResetVote(VotePayload p) {
    super(Upvote.class.getSimpleName());
    this.payload = p;
  }

}
