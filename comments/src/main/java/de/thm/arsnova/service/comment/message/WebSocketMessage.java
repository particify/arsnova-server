package de.thm.arsnova.service.comment.message;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class WebSocketMessage<P extends WebSocketPayload> implements Serializable {
    private String type;

    private P payload;

    public WebSocketMessage(String type) {
        this.type = type;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
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
        return "WebSocketMessage{" +
                "type='" + type + '\'' +
                ", payload=" + payload.toString() +
                '}';
    }
}
