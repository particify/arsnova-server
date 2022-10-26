package de.thm.arsnova.service.comment.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.Date;

@Entity
public class Comment {
    public static final int MAX_BODY_LENGTH = 500;
    public static final int MAX_ANSWER_LENGTH = 500;

    @Id
    private String id;
    private String roomId;
    private String creatorId;
    @Column(columnDefinition = "TEXT")
    private String body;
    private Date timestamp;
    private boolean read;
    private boolean favorite;
    private int correct;
    private boolean ack;
    @Transient
    private int score;
    private String tag;
    @Column(columnDefinition = "TEXT")
    private String answer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body.length() > MAX_BODY_LENGTH ? body.substring(0, MAX_BODY_LENGTH) : body;
    }

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

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public int getCorrect() {
        return correct;
    }

    public void setCorrect(int correct) {
        this.correct = correct;
    }

    public boolean isAck() {
        return ack;
    }

    public void setAck(boolean ack) {
        this.ack = ack;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer.length() > MAX_ANSWER_LENGTH ? answer.substring(0, MAX_ANSWER_LENGTH) : answer;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "id='" + id + '\'' +
                ", roomId='" + roomId + '\'' +
                ", creatorId='" + creatorId + '\'' +
                ", body='" + body + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + read +
                ", favorite=" + favorite +
                ", correct=" + correct +
                ", ack=" + ack +
                ", score=" + score +
                ", tag=" + tag +
                ", answer=" + answer +
                '}';
    }
}
