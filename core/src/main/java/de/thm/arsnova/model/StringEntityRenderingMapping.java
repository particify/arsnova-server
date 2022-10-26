package de.thm.arsnova.model;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class StringEntityRenderingMapping extends EntityRenderingMapping<String> {
	public StringEntityRenderingMapping(
			final Supplier<String> rawValueSupplier,
			final Consumer<String> renderedValueConsumer,
			final TextRenderingOptions options) {
		super(rawValueSupplier, renderedValueConsumer, options);
	}
}
