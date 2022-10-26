package de.thm.arsnova.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.thm.arsnova.model.Announcement;
import de.thm.arsnova.service.AnnouncementService;

@RestController
@EntityRequestMapping(AnnouncementController.REQUEST_MAPPING)
public class AnnouncementController extends AbstractEntityController<Announcement> {
	private AnnouncementService announcementService;

	protected AnnouncementController(
			@Qualifier("securedAnnouncementService") final AnnouncementService announcementService) {
		super(announcementService);
		this.announcementService = announcementService;
	}

	public static final String REQUEST_MAPPING = "/room/{roomId}/announcement";

	@Override
	protected String getMapping() {
		return REQUEST_MAPPING;
	}

	@GetMapping(value = GET_MULTIPLE_MAPPING, params = {"!roomIds"})
	public List<Announcement> getByRoomId(@PathVariable final String roomId) {
		return announcementService.getByRoomId(roomId);
	}

	@GetMapping(value = GET_MULTIPLE_MAPPING, params = {"roomIds"})
	public List<Announcement> getByRoomIds(@RequestParam final List<String> roomIds) {
		return announcementService.getByRoomIds(roomIds);
	}
}
