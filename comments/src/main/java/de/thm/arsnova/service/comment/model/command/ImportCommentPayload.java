package de.thm.arsnova.service.comment.model.command;

import java.util.Date;
import java.util.Objects;

public class ImportCommentPayload extends CreateCommentPayload {
    private Date timestamp;
    private boolean read;

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ImportCommentPayload that = (ImportCommentPayload) o;
        return read == that.read &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp, read);
    }
}
