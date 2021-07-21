package de.thm.arsnova.service.comment.model;

public class VoteSum {
    private String commentId;
    private int sum;

    public VoteSum(final String commentId, final int sum) {
        this.commentId = commentId;
        this.sum = sum;
    }

    public VoteSum(final String commentId, final long sum) {
        this.commentId = commentId;
        this.sum = (int) sum;
    }

    public String getCommentId() {
        return commentId;
    }

    public int getSum() {
        return sum;
    }
}
