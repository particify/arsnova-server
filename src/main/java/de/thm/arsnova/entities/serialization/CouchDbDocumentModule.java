/*
 * This file is part of ARSnova Backend.
 * Copyright (C) 2012-2018 The ARSnova Team and Contributors
 *
 * ARSnova Backend is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova Backend is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.entities.serialization;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.thm.arsnova.entities.Answer;
import de.thm.arsnova.entities.ChoiceAnswer;
import de.thm.arsnova.entities.ChoiceQuestionContent;
import de.thm.arsnova.entities.Content;
import de.thm.arsnova.entities.Entity;
import de.thm.arsnova.entities.TextAnswer;

public class CouchDbDocumentModule extends SimpleModule {
	public CouchDbDocumentModule() {
		super("CouchDbDocumentModule");
	}

	@Override
	public void setupModule(SetupContext context) {
		context.setMixInAnnotations(Entity.class, CouchDbDocumentMixIn.class);
		context.setMixInAnnotations(de.thm.arsnova.entities.migration.v2.Entity.class, CouchDbDocumentV2MixIn.class);
		context.registerSubtypes(
				new NamedType(Content.class, Content.class.getSimpleName()),
				new NamedType(ChoiceQuestionContent.class, ChoiceQuestionContent.class.getSimpleName()),
				new NamedType(Answer.class, Answer.class.getSimpleName()),
				new NamedType(ChoiceAnswer.class, ChoiceAnswer.class.getSimpleName()),
				new NamedType(TextAnswer.class, TextAnswer.class.getSimpleName()));
	}
}
