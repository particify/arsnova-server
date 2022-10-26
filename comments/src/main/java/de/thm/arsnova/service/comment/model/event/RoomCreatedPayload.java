package de.thm.arsnova.service.comment.model.event;

import de.thm.arsnova.service.comment.model.WebSocketPayload;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class RoomCreatedPayload implements WebSocketPayload {

    public static class Moderator {
        public enum Role {
            EDITING_MODERATOR,
            EXECUTIVE_MODERATOR
        }

        private String userId;
        private Set<Role> roles;

        public String getUserId() {
            return userId;
        }

        public void setUserId(final String userId) {
            this.userId = userId;
        }

        public Set<Role> getRoles() {
            if (roles == null) {
                roles = new HashSet<>();
            }

            return roles;
        }

        public void setRoles(final Set<Role> roles) {
            this.roles = roles;
        }

        @Override
        public String toString() {
            return "Moderator{" +
                    "userId='" + userId + '\'' +
                    ", roles=" + roles +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Moderator moderator = (Moderator) o;
            return Objects.equals(userId, moderator.userId) &&
                    Objects.equals(roles, moderator.roles);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roles);
        }
    }

    private String id;
    private String ownerId;
    private Set<Moderator> moderators;

    public RoomCreatedPayload() {
    }
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Set<Moderator> getModerators() {
        return moderators;
    }

    public void setModerators(Set<Moderator> moderators) {
        this.moderators = moderators;
    }

    @Override
    public String toString() {
        return "RoomCreatedPayload{" +
                "id='" + id + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", moderators=" + moderators +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomCreatedPayload that = (RoomCreatedPayload) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(moderators, that.moderators);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId, moderators);
    }
}
