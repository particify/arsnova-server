package net.particify.arsnova.comments.model.command;

public class Upvote extends WebSocketCommand<VotePayload> {
  public Upvote() {
    super(Upvote.class.getSimpleName());
  }

  public Upvote(VotePayload p) {
    super(Upvote.class.getSimpleName());
    this.payload = p;
  }

}
