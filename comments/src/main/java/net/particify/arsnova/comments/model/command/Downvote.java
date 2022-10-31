package net.particify.arsnova.comments.model.command;

public class Downvote extends WebSocketCommand<VotePayload> {
  public Downvote() {
    super(Downvote.class.getSimpleName());
  }

  public Downvote(VotePayload p) {
    super(Downvote.class.getSimpleName());
    this.payload = p;
  }

}