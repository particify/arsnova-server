package de.thm.arsnova.model;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.core.style.ToStringCreator;

public class EntityRenderingMapping {
	private Supplier<String> rawValueSupplier;
	private Consumer<String> renderedValueConsumer;
	private TextRenderingOptions options;

	public EntityRenderingMapping() {
	}

	public EntityRenderingMapping(
			final Supplier<String> rawValueSupplier,
			final Consumer<String> renderedValueConsumer, final TextRenderingOptions options) {
		this.rawValueSupplier = rawValueSupplier;
		this.renderedValueConsumer = renderedValueConsumer;
		this.options = options;
	}

	public Supplier<String> getRawValueSupplier() {
		return rawValueSupplier;
	}

	public void setRawValueSupplier(final Supplier<String> rawValueSupplier) {
		this.rawValueSupplier = rawValueSupplier;
	}

	public Consumer<String> getRenderedValueConsumer() {
		return renderedValueConsumer;
	}

	public void setRenderedValueConsumer(final Consumer<String> renderedValueConsumer) {
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
