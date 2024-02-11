package net.particify.arsnova.core.model.export;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.text.DecimalFormat;
import java.util.InputMismatchException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.particify.arsnova.core.model.ChoiceQuestionContent;
import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.NumericContent;
import net.particify.arsnova.core.model.PrioritizationChoiceContent;
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
  private static final DecimalFormat decimalFormat = new DecimalFormat("#.#");

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
    } else if (content instanceof NumericContent numericContent) {
      // Minus sign (U+2212) is used to separate min and max.
      this.options = List.of(
          decimalFormat.format(numericContent.getMinNumber()) + '−'
              + decimalFormat.format(numericContent.getMaxNumber()));
      if (numericContent.getCorrectNumber() != null) {
        // Plus-minus sign (U+00B1) is used to separate correctNumber and tolerance.
        this.correctOptions = List.of(
            decimalFormat.format(numericContent.getCorrectNumber()) + '±'
                + decimalFormat.format(numericContent.getTolerance()));
      }
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
        || format == Content.Format.SORT
        || format == Content.Format.PRIORITIZATION) {
      content = toChoiceContent();
    } else if (format == Content.Format.SCALE) {
      try {
        content = toScaleContent();
      } catch (final InputMismatchException e) {
        content = toChoiceContent();
        format = Content.Format.CHOICE;
      }
    } else if (format == Content.Format.NUMERIC) {
      content = toNumericContent();
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
    if (this.options.size() < 2) {
      throw new InputMismatchException();
    }
    final ChoiceQuestionContent choiceQuestionContent = switch (this.format) {
      case PRIORITIZATION -> new PrioritizationChoiceContent();
      default -> new ChoiceQuestionContent();
    };
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

  private NumericContent toNumericContent() {
    final NumericContent numericContent = new NumericContent();
    if (this.options.size() != 1 || this.correctOptions.size() > 1) {
      throw new InputMismatchException();
    }
    // Allow minus and similar signs as separator.
    final String[] range = this.options.get(0).split("[−–-]");
    numericContent.setMinNumber(Double.parseDouble(range[0]));
    numericContent.setMaxNumber(Double.parseDouble(range[1]));
    if (!this.correctOptions.isEmpty()) {
      // Allow plus-minus and compositions of plus and minus signs as separator.
      final String[] correctAndTolerance = this.correctOptions.get(0).split("±|\\+[−–-]");
      numericContent.setCorrectNumber(Double.parseDouble(correctAndTolerance[0]));
      numericContent.setTolerance(Double.parseDouble(correctAndTolerance[1]));
    }

    return numericContent;
  }

  private WordcloudContent toWordcloudContent() {
    final WordcloudContent wordcloudContent = new WordcloudContent();
    final int maxAnswers;
    if (this.getOptions().size() != 1) {
      throw new InputMismatchException();
    }
    try {
      maxAnswers = Integer.parseInt(this.getOptions().get(0));
    } catch (final NumberFormatException e) {
      throw new InputMismatchException();
    }
    wordcloudContent.setMaxAnswers(maxAnswers);

    return wordcloudContent;
  }
}
