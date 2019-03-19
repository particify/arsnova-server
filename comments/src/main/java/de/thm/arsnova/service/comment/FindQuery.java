package de.thm.arsnova.service.comment;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.thm.arsnova.service.comment.model.Comment;
import org.springframework.core.style.ToStringCreator;

import java.util.Map;

public class FindQuery {
    enum LogicalOperator {
        AND,
        OR
    }

    private LogicalOperator operator = LogicalOperator.AND;
    private Comment properties;
    private Map<String, Object> externalFilters;

    @JsonProperty("operator")
    public LogicalOperator getOperator() {
        return operator;
    }

    @JsonProperty("operator")
    public void setOperator(final LogicalOperator operator) {
        this.operator = operator;
    }

    @JsonProperty("properties")
    public Comment getProperties() {
        return properties;
    }

    @JsonProperty("properties")
    public void setProperties(final Comment properties) {
        this.properties = properties;
    }

    @JsonProperty("externalFilters")
    public Map<String, Object> getExternalFilters() {
        return externalFilters;
    }

    @JsonProperty("externalFilters")
    public void setExternalFilters(final Map<String, Object> externalFilters) {
        this.externalFilters = externalFilters;
    }

    @Override
    public String toString() {
        return new ToStringCreator(getClass())
                .append("operator", operator)
                .append("properties", properties)
                .append("externalFilters", externalFilters)
                .toString();
    }
}
