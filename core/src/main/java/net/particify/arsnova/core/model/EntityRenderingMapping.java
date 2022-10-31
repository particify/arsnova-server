package net.particify.arsnova.core.model;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.core.style.ToStringCreator;

public abstract class EntityRenderingMapping<T> {
  private Supplier<T> rawValueSupplier;
  private Consumer<T> renderedValueConsumer;
  private TextRenderingOptions options;

  public EntityRenderingMapping() {
  }

  public EntityRenderingMapping(
      final Supplier<T> rawValueSupplier,
      final Consumer<T> renderedValueConsumer, final TextRenderingOptions options) {
    this.rawValueSupplier = rawValueSupplier;
    this.renderedValueConsumer = renderedValueConsumer;
    this.options = options;
  }

  public Supplier<T> getRawValueSupplier() {
    return rawValueSupplier;
  }

  public void setRawValueSupplier(final Supplier<T> rawValueSupplier) {
    this.rawValueSupplier = rawValueSupplier;
  }

  public Consumer<T> getRenderedValueConsumer() {
    return renderedValueConsumer;
  }

  public void setRenderedValueConsumer(final Consumer<T> renderedValueConsumer) {
    this.renderedValueConsumer = renderedValueConsumer;
  }

  public TextRenderingOptions getOptions() {
    return options;
  }

  public void setOptions(final TextRenderingOptions options) {
    this.options = options;
  }

  @Override
  public String toString() {
    return new ToStringCreator(this)
        .append("options", options)
        .toString();
  }
}
