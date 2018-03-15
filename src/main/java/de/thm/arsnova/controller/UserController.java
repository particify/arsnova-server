package de.thm.arsnova.controller;

import de.thm.arsnova.entities.LoginCredentials;
import de.thm.arsnova.entities.UserProfile;
import de.thm.arsnova.services.RoomService;
import de.thm.arsnova.services.UserService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(UserController.REQUEST_MAPPING)
public class UserController extends AbstractEntityController<UserProfile> {
	protected static final String REQUEST_MAPPING = "/user";
	private static final String ROOM_HISTORY_MAPPING = DEFAULT_ID_MAPPING + "/roomHistory";

	private UserService userService;
	private RoomService roomService;

	public UserController(final UserService userService, final RoomService roomService) {
		super(userService);
		this.userService = userService;
		this.roomService = roomService;
	}

	@Override
	protected String getMapping() {
		return REQUEST_MAPPING;
	}

	@PostMapping("/register")
	public void register(@RequestBody LoginCredentials loginCredentials) {
		userService.create(loginCredentials.getLoginId(), loginCredentials.getPassword());
	}

	@PostMapping(ROOM_HISTORY_MAPPING)
	public void postRoomHistoryEntry(@PathVariable final String id,
			@RequestBody final UserProfile.RoomHistoryEntry roomHistoryEntry) {
		userService.addRoomToHistory(userService.get(id), roomService.get(roomHistoryEntry.getRoomId()));
	}
}
