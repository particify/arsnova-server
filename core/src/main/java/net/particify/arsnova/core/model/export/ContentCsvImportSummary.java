package net.particify.arsnova.core.model.export;

import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;

import net.particify.arsnova.core.model.serialization.View;

@JsonView(View.Public.class)
public record ContentCsvImportSummary(
    int totalLines,
    int importedLines,
    List<Integer> errorLines) {
}
