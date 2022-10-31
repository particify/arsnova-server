package net.particify.arsnova.core.model.export;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.InputMismatchException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.particify.arsnova.core.model.ChoiceQuestionContent;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ScaleChoiceContent;
import net.particify.arsnova.core.model.WordcloudContent;
import net.particify.arsnova.core.model.serialization.View;

@JsonView(View.Public.class)
@JsonPropertyOrder({
    "format",
    "body",
    "additionalText",
    "options",
    "correctOptions",
    "multiple",
    "abstentionsAllowed"
})
public class ContentExport {
  private static final String SCALE_OPTION_FORMAT = "%s (%d)";
  private static final Pattern SCALE_TEMPLATE_PATTERN = Pattern.compile("^([A-Z_]+) \\(([45])\\)$");

  private Content.Format format;
  private String body;
  private String additionalText;
  private List<String> options;
  private List<String> correctOptions;
  private boolean multiple;
  private boolean abstentionsAllowed;

  public ContentExport() {

  }

  public ContentExport(final Content content) {
    this.format = content.getFormat();
    this.body = content.getBody();
    this.additionalText = content.getAdditionalText();
    this.abstentionsAllowed = content.isAbstentionsAllowed();
    if (content instanceof ScaleChoiceContent) {
      final ScaleChoiceContent scaleChoiceContent = (ScaleChoiceContent) content;
      this.options = List.of(String.format(
          SCALE_OPTION_FORMAT,
          scaleChoiceContent.getOptionTemplate(),
          scaleChoiceContent.getOptionCount()));
    } else if (content instanceof ChoiceQuestionContent) {
      final ChoiceQuestionContent choiceQuestionContent = (ChoiceQuestionContent) content;
      this.options = choiceQuestionContent.getOptions().stream()
          .map(o -> o.getLabel())
          .collect(Collectors.toList());
      this.correctOptions = IntStream.range(0, this.options.size())
          .filter(i -> choiceQuestionContent.getCorrectOptionIndexes().contains(i))
          .mapToObj(i -> this.options.get(i))
          .collect(Collectors.toList());
      this.multiple = choiceQuestionContent.isMultiple();
    } else if (content instanceof WordcloudContent) {
      final WordcloudContent wordcloudContent = (WordcloudContent) content;
      this.options = List.of(String.valueOf(wordcloudContent.getMaxAnswers()));
    }
  }

  public Content toContent() {
    Content content;
    Content.Format format = this.format;
    if (format == Content.Format.CHOICE
        || format == Content.Format.BINARY
        || format == Content.Format.SORT) {
      content = toChoiceContent();
    } else if (format == Content.Format.SCALE) {
      try {
        content = toScaleContent();
      } catch (final InputMismatchException e) {
        content = toChoiceContent();
        format = Content.Format.CHOICE;
      }
    } else if (format == Content.Format.WORDCLOUD) {
      content = toWordcloudContent();
    } else {
      content = new Content();
    }
    content.setFormat(format);
    content.setBody(this.body);
    content.setAdditionalText(this.additionalText);
    content.setAbstentionsAllowed(this.abstentionsAllowed);

    return content;
  }

  public Content.Format getFormat() {
    return format;
  }

  public String getBody() {
    return body;
  }

  public String getAdditionalText() {
    return additionalText;
  }

  public List<String> getOptions() {
    return options;
  }

  public List<String> getCorrectOptions() {
    return correctOptions;
  }

  public boolean isMultiple() {
    return multiple;
  }

  public boolean isAbstentionsAllowed() {
    return abstentionsAllowed;
  }

  private ChoiceQuestionContent toChoiceContent() {
    final ChoiceQuestionContent choiceQuestionContent = new ChoiceQuestionContent();
    choiceQuestionContent.setOptions(
        this.options.stream()
            .map(o -> new ChoiceQuestionContent.AnswerOption(o))
            .collect(Collectors.toList()));
    choiceQuestionContent.setCorrectOptionIndexes(
        this.correctOptions.stream()
            .map(o -> this.options.indexOf(o))
            .filter(i -> i >= 0)
            .collect(Collectors.toList()));
    choiceQuestionContent.setMultiple(this.multiple);

    return choiceQuestionContent;
  }

  private ScaleChoiceContent toScaleContent() {
    final ScaleChoiceContent scaleChoiceContent = new ScaleChoiceContent();
    if (this.options.size() != 1) {
      throw new InputMismatchException();
    }
    final Matcher templateMatcher = SCALE_TEMPLATE_PATTERN.matcher(this.options.get(0));
    if (!templateMatcher.matches()) {
      throw new InputMismatchException();
    }
    final int optionCount = Integer.parseInt(templateMatcher.group(2));

    final ScaleChoiceContent.ScaleOptionTemplate template =
        ScaleChoiceContent.ScaleOptionTemplate.valueOf(templateMatcher.group(1));
    scaleChoiceContent.setOptionTemplate(template);
    scaleChoiceContent.setOptionCount(optionCount);

    return scaleChoiceContent;
  }

  private WordcloudContent toWordcloudContent() {
    final WordcloudContent wordcloudContent = new WordcloudContent();
    final int maxAnswers;
    if (this.getOptions().size() != 1) {
      throw new InputMismatchException();
    }
    try {
      maxAnswers = Integer.valueOf(this.getOptions().get(0));
    } catch (final NumberFormatException e) {
      throw new InputMismatchException();
    }
    wordcloudContent.setMaxAnswers(maxAnswers);

    return wordcloudContent;
  }
}
