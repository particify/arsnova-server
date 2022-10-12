package de.thm.arsnova.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.service.FeedbackStorageService;

@RestController
public class SurveyController {
	protected static final String REQUEST_MAPPING = "/survey";
	private static final String GET_SURVEY_BY_ROOM_MAPPING =
			"/room" + AbstractEntityController.DEFAULT_ID_MAPPING + REQUEST_MAPPING;

	private FeedbackStorageService service;

	public SurveyController(
			final FeedbackStorageService service
	) {
		this.service = service;
	}

	@GetMapping(GET_SURVEY_BY_ROOM_MAPPING)
	public int[] getByRoom(@PathVariable final String id) {
		// Fake room for ref management
		final Room room = new Room();
		room.setId(id);

		final Feedback feedback = service.getByRoom(room);
		final int[] currentVals = feedback.getValues().stream().mapToInt(i -> i).toArray();

		return currentVals;
	}
}
