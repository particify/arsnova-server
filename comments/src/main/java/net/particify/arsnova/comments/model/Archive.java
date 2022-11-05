package net.particify.arsnova.comments.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.util.Objects;
import java.util.Set;

@Entity
public class Archive {
  @Id
  private String id;
  private String roomId;
  private String name;
  @Transient
  private Set<Comment> comments;
  @Transient
  private long count;

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

  public long getCount() {
    return count;
  }

  public void setCount(final long count) {
    this.count = count;
  }

  @Override
  public String toString() {
    return "Archive{" +
        "id='" + id + '\'' +
        ", roomId='" + roomId + '\'' +
        ", name='" + name + '\'' +
        ", comments=" + comments +
        ", count=" + count +
        '}';
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Archive archive = (Archive) o;
    return count == archive.count && Objects.equals(id, archive.id) && Objects.equals(
        roomId,
        archive.roomId) && Objects.equals(name, archive.name) && Objects.equals(
        comments,
        archive.comments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, roomId, name, comments, count);
  }
}
