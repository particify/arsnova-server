package net.particify.arsnova.core.model;

import com.fasterxml.jackson.annotation.JsonView;

import net.particify.arsnova.core.model.serialization.View;

@JsonView(View.Public.class)
public record AnswerStatisticsSummaryEntry(
    String contentId,
    int round,
    AnswerResult.AnswerResultState result,
    int count) {}
