package net.particify.arsnova.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ContentTest {
  @Test
  public void testDetermineAnswerResultContentIdIsSet() {
    final ChoiceQuestionContent content = buildChoiceQuestionContent(Content.Format.CHOICE, 1, 3, 5);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(content.getId(), answerResult.getContentId());
  }

  @Test
  public void testDetermineAnswerResultAbstainedWithChoiceFormat() {
    final ChoiceQuestionContent content = buildChoiceQuestionContent(Content.Format.CHOICE, 1, 3, 5);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.ABSTAINED, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultCorrectWithChoiceFormat() {
    final ChoiceQuestionContent content = buildChoiceQuestionContent(Content.Format.CHOICE, 1, 3, 5);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setSelectedChoiceIndexes(List.of(1, 3, 5));

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(500, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.CORRECT, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultWrongWithChoiceFormat() {
    final ChoiceQuestionContent content = buildChoiceQuestionContent(Content.Format.CHOICE, 1, 3, 5);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setSelectedChoiceIndexes(List.of(2, 4));

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.WRONG, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultPartiallyWrongWithChoiceFormat() {
    final ChoiceQuestionContent content = buildChoiceQuestionContent(Content.Format.CHOICE, 1, 3, 5);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setSelectedChoiceIndexes(List.of(1, 2, 3, 5));

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    // 10 points total, 3 correct options, 3 correct options chosen, 1 incorrect option chosen
    assertEquals(500.0 / 3 * (3 - 1), answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.WRONG, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultNeutralWithChoiceFormat() {
    final ChoiceQuestionContent content = buildChoiceQuestionContent(Content.Format.CHOICE);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setSelectedChoiceIndexes(List.of(1, 3));

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.NEUTRAL, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultCorrectWithSortFormat() {
    final ChoiceQuestionContent content = buildChoiceQuestionContent(Content.Format.SORT, 1, 2, 3, 4);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setSelectedChoiceIndexes(List.of(1, 2, 3, 4));

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(500, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.CORRECT, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultWrongWithSortFormat() {
    final ChoiceQuestionContent content = buildChoiceQuestionContent(Content.Format.SORT, 1, 2, 3, 4);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setSelectedChoiceIndexes(List.of(1, 2, 4, 3));

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.WRONG, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultNeutralWithScaleFormat() {
    final ScaleChoiceContent content = new ScaleChoiceContent();
    content.setId("ID-1");
    content.setFormat(Content.Format.SCALE);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setSelectedChoiceIndexes(List.of(1, 3));

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.NEUTRAL, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultAbstainedWithScaleFormat() {
    final ScaleChoiceContent content = new ScaleChoiceContent();
    content.setId("ID-1");
    content.setFormat(Content.Format.SCALE);
    final ChoiceAnswer answer = new ChoiceAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.ABSTAINED, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultNeutralWithTextFormat() {
    final Content content = new Content();
    content.setId("ID-1");
    content.setFormat(Content.Format.TEXT);

    final TextAnswer answer = new TextAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setBody("Body");

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.NEUTRAL, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultAbstainedWithTextFormat() {
    final Content content = new Content();
    content.setId("ID-1");
    content.setFormat(Content.Format.TEXT);

    final TextAnswer answer = new TextAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setBody(null);

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.ABSTAINED, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultNeutralWithWordcloudFormat() {
    final Content content = new Content();
    content.setId("ID-1");
    content.setFormat(Content.Format.WORDCLOUD);

    final MultipleTextsAnswer answer = new MultipleTextsAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());
    answer.setTexts(List.of("Word-1", "Word-2"));

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.NEUTRAL, answerResult.getState());
  }

  @Test
  public void testDetermineAnswerResultAbstainedWithWordcloudFormat() {
    final Content content = new Content();
    content.setId("ID-1");
    content.setFormat(Content.Format.WORDCLOUD);

    final MultipleTextsAnswer answer = new MultipleTextsAnswer(content, "User-1");
    answer.setCreationTimestamp(new Date());

    final AnswerResult answerResult = content.determineAnswerResult(answer);
    assertEquals(0, answerResult.getAchievedPoints(), 0.01);
    assertEquals(AnswerResult.AnswerResultState.ABSTAINED, answerResult.getState());
  }

  @Test
  public void testCalculateCompetitivePoints() {
    final Content content = new Content();
    content.setId("ID-1");
    content.setFormat(Content.Format.CHOICE);
    content.setDuration(20);
    final Instant endTime = Instant.now();
    final Instant answerTime = endTime.minus(1200, ChronoUnit.MILLIS);
    final Content.State state = content.getState();
    state.setAnsweringEndTime(Date.from(endTime));
    assertEquals(60, Math.round(content.calculateCompetitivePoints(answerTime, 500)));
  }

  private ChoiceQuestionContent buildChoiceQuestionContent(
      final Content.Format format, final Integer... options) {
    final ChoiceQuestionContent content = new ChoiceQuestionContent();
    content.setId("ID-1");
    content.setFormat(format);
    content.setOptions(List.of(
        new ChoiceQuestionContent.AnswerOption("Opt-1"),
        new ChoiceQuestionContent.AnswerOption("Opt-2"),
        new ChoiceQuestionContent.AnswerOption("Opt-3"),
        new ChoiceQuestionContent.AnswerOption("Opt-4"),
        new ChoiceQuestionContent.AnswerOption("Opt-5")
    ));
    content.setCorrectOptionIndexes(List.of(options));

    return content;
  }
}
