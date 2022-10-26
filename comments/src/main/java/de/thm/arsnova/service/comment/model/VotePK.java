package de.thm.arsnova.service.comment.model;

import java.io.Serializable;
import java.util.Objects;

public class VotePK implements Serializable {
    protected String userId;
    protected String commentId;

    public VotePK() {
    }

    public VotePK(final String userId, final String commentId) {
        this.userId = userId;
        this.commentId = commentId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final VotePK votePK = (VotePK) o;
        return Objects.equals(userId, votePK.userId) &&
                Objects.equals(commentId, votePK.commentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, commentId);
    }
}
