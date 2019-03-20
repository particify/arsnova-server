package de.thm.arsnova.service.comment.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.arsnova.service.comment.model.WebSocketPayload;
import de.thm.arsnova.service.comment.model.command.WebSocketCommand;

public class WebSocketEvent<P extends WebSocketPayload> extends WebSocketCommand<P> {
    // roomId of the entity the event is based on
    protected String roomId;
    protected P payload;

    public WebSocketEvent(String type) {
        super(type);
    }

    public WebSocketEvent(String type, String roomId) {
        super(type);
        this.roomId = roomId;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("roomId")
    public String getRoomId() {
        return roomId;
    }

    @JsonProperty("roomId")
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    @JsonProperty("payload")
    public P getPayload() {
        return payload;
    }

    @JsonProperty("payload")
    public void setPayload(P payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "WebSocketEvent{" +
                "type='" + type + '\'' +
                ", payload=" + payload.toString() +
                '}';
    }
}
