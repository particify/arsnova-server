package de.thm.arsnova.controller;

import static de.thm.arsnova.controller.AbstractEntityController.ENTITY_ID_HEADER;
import static de.thm.arsnova.controller.AbstractEntityController.ENTITY_REVISION_HEADER;

import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.transport.ImportExportContainer;
import de.thm.arsnova.service.ImportService;

@RestController
@RequestMapping(ImportController.REQUEST_MAPPING)
public class ImportController {
	protected static final String REQUEST_MAPPING = "/import";
	private static final String IMPORT_V2_ROOM_MAPPING = "/v2/room";

	private ImportService importService;

	public ImportController(final ImportService importService) {
		this.importService = importService;
	}

	@PostMapping(IMPORT_V2_ROOM_MAPPING)
	public Room importRoom(
			@RequestBody final ImportExportContainer container,
			final HttpServletResponse httpServletResponse
	) {
		final Room room = importService.importFromV2(container);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, room.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, room.getRevision());

		return room;
	}
}
