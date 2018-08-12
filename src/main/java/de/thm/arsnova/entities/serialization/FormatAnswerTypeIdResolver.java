package de.thm.arsnova.entities.serialization;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.type.TypeFactory;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.ChoiceAnswer;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.TextAnswer;

import java.io.IOException;

public class FormatAnswerTypeIdResolver extends TypeIdResolverBase {
	@Override
	public String idFromValue(final Object value) {
		if (value instanceof Answer) {
			return ((Answer) value).getFormat().toString();
		} else {
			throw new IllegalArgumentException("Unsupported type.");
		}
	}

	@Override
	public String idFromValueAndType(final Object value, final Class<?> suggestedType) {
		return idFromValue(value);
	}

	@Override
	public JavaType typeFromId(final DatabindContext context, final String id) throws IOException {
		Content.Format format = Content.Format.valueOf(id);
		switch (format) {
			case BINARY:
				return TypeFactory.defaultInstance().constructType(ChoiceAnswer.class);
			case CHOICE:
				return TypeFactory.defaultInstance().constructType(ChoiceAnswer.class);
			case NUMBER:
				return TypeFactory.defaultInstance().constructType(ChoiceAnswer.class);
			case SCALE:
				return TypeFactory.defaultInstance().constructType(ChoiceAnswer.class);
			case TEXT:
				return TypeFactory.defaultInstance().constructType(TextAnswer.class);
			default:
				throw new IllegalArgumentException("Unsupported type ID.");
		}
	}

	@Override
	public JsonTypeInfo.Id getMechanism() {
		return JsonTypeInfo.Id.CUSTOM;
	}
}
