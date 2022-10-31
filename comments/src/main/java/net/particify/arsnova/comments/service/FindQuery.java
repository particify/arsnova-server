package net.particify.arsnova.comments.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.springframework.core.style.ToStringCreator;

public class FindQuery<E> {
  enum LogicalOperator {
    AND,
    OR
  }

  private LogicalOperator operator = LogicalOperator.AND;
  private E properties;
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
  public E getProperties() {
    return properties;
  }

  @JsonProperty("properties")
  public void setProperties(final E properties) {
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
