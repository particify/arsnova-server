package de.thm.arsnova.model;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Positive;

import de.thm.arsnova.model.serialization.View;

public class GridImageContent extends Content {
  public static class Grid {
    @Positive
    private int columns;

    @Positive
    private int rows;

    @Min(-1)
    @Max(1)
    private double normalizedX;

    @Min(-1)
    @Max(1)
    private double normalizedY;

    @Positive
    @Max(1)
    private double normalizedFieldSize;

    private boolean visible;

    @JsonView({View.Persistence.class, View.Public.class})
    public int getColumns() {
      return columns;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setColumns(final int columns) {
      this.columns = columns;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public int getRows() {
      return rows;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setRows(final int rows) {
      this.rows = rows;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public double getNormalizedX() {
      return normalizedX;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setNormalizedX(final double normalizedX) {
      this.normalizedX = normalizedX;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public double getNormalizedY() {
      return normalizedY;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setNormalizedY(final double normalizedY) {
      this.normalizedY = normalizedY;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public double getNormalizedFieldSize() {
      return normalizedFieldSize;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setNormalizedFieldSize(final double normalizedFieldSize) {
      this.normalizedFieldSize = normalizedFieldSize;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public boolean isVisible() {
      return visible;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setVisible(final boolean visible) {
      this.visible = visible;
    }
  }

  public static class Image {
    private String url;

    @Min(-1)
    @Max(1)
    private double normalizedX;

    @Min(-1)
    @Max(1)
    private double normalizedY;

    @Positive
    private double scaleFactor;

    @Min(0)
    @Max(360)
    private int rotation;

    @JsonView({View.Persistence.class, View.Public.class})
    public String getUrl() {
      return url;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setUrl(final String url) {
      this.url = url;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public double getNormalizedX() {
      return normalizedX;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setNormalizedX(final double normalizedX) {
      this.normalizedX = normalizedX;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public double getNormalizedY() {
      return normalizedY;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setNormalizedY(final double normalizedY) {
      this.normalizedY = normalizedY;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public double getScaleFactor() {
      return scaleFactor;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setScaleFactor(final double scaleFactor) {
      this.scaleFactor = scaleFactor;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public int getRotation() {
      return rotation;
    }

    @JsonView({View.Persistence.class, View.Public.class})
    public void setRotation(final int rotation) {
      this.rotation = rotation;
    }
  }

  private Grid grid;
  private Image image;
  private List<Integer> correctOptionIndexes = new ArrayList<>();

  public GridImageContent() {

  }

  public GridImageContent(final GridImageContent content) {
    super(content);
    this.grid = content.grid;
    this.image = content.image;
    this.correctOptionIndexes = content.correctOptionIndexes;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Grid getGrid() {
    if (grid == null) {
      grid = new Grid();
    }

    return grid;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setGrid(final Grid grid) {
    this.grid = grid;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public Image getImage() {
    if (image == null) {
      image = new Image();
    }

    return image;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setImage(final Image image) {
    this.image = image;
  }

  /* TODO: A new JsonView is needed here. */
  @JsonView(View.Persistence.class)
  public List<Integer> getCorrectOptionIndexes() {
    return correctOptionIndexes;
  }

  @JsonView({View.Persistence.class, View.Public.class})
  public void setCorrectOptionIndexes(final List<Integer> correctOptionIndexes) {
    this.correctOptionIndexes = correctOptionIndexes;
  }
}
