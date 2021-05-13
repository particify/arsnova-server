package de.thm.arsnova.model.export;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.thm.arsnova.model.ChoiceQuestionContent;
import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.serialization.View;

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
		if (content instanceof ChoiceQuestionContent) {
			final ChoiceQuestionContent choiceQuestionContent = (ChoiceQuestionContent) content;
			this.options = choiceQuestionContent.getOptions().stream()
					.map(o -> o.getLabel())
					.collect(Collectors.toList());
			this.correctOptions = IntStream.range(0, this.options.size())
					.filter(i -> choiceQuestionContent.getCorrectOptionIndexes().contains(i))
					.mapToObj(i -> this.options.get(i))
					.collect(Collectors.toList());
			this.multiple = choiceQuestionContent.isMultiple();
		}
	}

	public Content toContent() {
		final Content content;
		if (this.format == Content.Format.CHOICE
				|| this.format == Content.Format.BINARY
				|| this.format == Content.Format.SCALE
				|| this.format == Content.Format.SORT) {
			final ChoiceQuestionContent choiceQuestionContent = new ChoiceQuestionContent();
			content = choiceQuestionContent;
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
		} else {
			content = new Content();
		}
		content.setFormat(this.format);
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
}
