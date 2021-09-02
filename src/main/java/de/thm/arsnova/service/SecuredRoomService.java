package de.thm.arsnova.service;

import java.util.List;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.RoomMembership;

@Service
public class SecuredRoomService extends AbstractSecuredEntityServiceImpl<Room>
		implements RoomService, SecuredService {
	private final RoomService roomService;

	public SecuredRoomService(final RoomService roomService) {
		super(Room.class, roomService);
		this.roomService = roomService;
	}

	@Override
	@PreAuthorize("permitAll")
	public String getIdByShortId(final String shortId) {
		return roomService.getIdByShortId(shortId);
	}

	@Override
	@PreAuthorize("hasPermission(#userId, 'userprofile', 'owner')")
	public List<String> getUserRoomIds(final String userId) {
		return roomService.getUserRoomIds(userId);
	}

	@Override
	@PreAuthorize("hasPermission(#userId, 'userprofile', 'read')")
	public List<Room> getUserRoomHistory(final String userId) {
		return roomService.getUserRoomHistory(userId);
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public Room transferOwnership(final Room room, final String newOwnerId) {
		return roomService.transferOwnership(room, newOwnerId);
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'owner')")
	public Room transferOwnershipThroughToken(final Room room, final String targetUserToken) {
		return roomService.transferOwnershipThroughToken(room, targetUserToken);
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'owner')")
	public String getPassword(final Room room) {
		return roomService.getPassword(room);
	}

	@Override
	@PreAuthorize("hasPermission(#room, 'owner')")
	public void setPassword(final Room room, final String password) {
		roomService.setPassword(room, password);
	}

	@Override
	@PreAuthorize("permitAll")
	public Optional<RoomMembership> requestMembership(final String roomId, final String password) {
		return roomService.requestMembership(roomId, password);
	}

	@Override
	@PreAuthorize("permitAll")
	public String generateShortId() {
		return roomService.generateShortId();
	}
}
