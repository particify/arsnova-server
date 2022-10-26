package de.thm.arsnova.model;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ListEntityRenderingMapping extends EntityRenderingMapping<List<String>> {
	public ListEntityRenderingMapping(
				final Supplier<List<String>> rawValueSupplier,
				final Consumer<List<String>> renderedValueConsumer,
				final TextRenderingOptions options) {
		super(rawValueSupplier, renderedValueConsumer, options);
	}
}
