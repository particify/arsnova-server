package de.thm.arsnova.service.comment.model.event;

import de.thm.arsnova.service.comment.model.Vote;
import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.Objects;

public class VoteCreatedPayload implements WebSocketPayload {
    private String commentId;
    private int vote;

    public VoteCreatedPayload() {
    }

    public VoteCreatedPayload(Vote v) {
        if (v != null) {
            commentId = v.getCommentId();
            vote = v.getVote();
        }
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public int getVote() {
        return vote;
    }

    public void setVote(int vote) {
        this.vote = vote;
    }

    @Override
    public String toString() {
        return "VoteCreatedPayload{" +
                ", commentId='" + commentId + '\'' +
                ", vote=" + vote +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoteCreatedPayload that = (VoteCreatedPayload) o;
        return vote == that.vote &&
                Objects.equals(commentId, that.commentId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(commentId, vote);
    }
}
