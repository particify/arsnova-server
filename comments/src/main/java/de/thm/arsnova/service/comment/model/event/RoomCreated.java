package de.thm.arsnova.service.comment.model.event;

public class RoomCreated extends WebSocketEvent<RoomCreatedPayload> {
    public RoomCreated() {
        super(RoomCreated.class.getSimpleName());
    }

    public RoomCreated(RoomCreatedPayload p, String id) {
        super(RoomCreated.class.getSimpleName(), id);
        this.payload = p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomCreated that = (RoomCreated) o;
        return this.getPayload().equals(that.getPayload());
    }
}
