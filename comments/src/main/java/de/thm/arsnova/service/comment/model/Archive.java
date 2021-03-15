package de.thm.arsnova.service.comment.model;

import java.util.Objects;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

@Entity
public class Archive {
    @Id
    private String id;
    private String roomId;
    private String name;
    @Transient
    private Set<Comment> comments;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(final String roomId) {
        this.roomId = roomId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Set<Comment> getComments() {
        return comments;
    }

    public void setComments(final Set<Comment> comments) {
        this.comments = comments;
    }

    @Override
    public String toString() {
        return "Archive{" +
                "id='" + id + '\'' +
                ", roomId='" + roomId + '\'' +
                ", name='" + name + '\'' +
                ", comments=" + comments +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Archive archive = (Archive) o;
        return Objects.equals(id, archive.id) && Objects.equals(
                roomId,
                archive.roomId) && Objects.equals(name, archive.name) && Objects.equals(
                comments,
                archive.comments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, roomId, name, comments);
    }
}
