package de.thm.arsnova.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import de.thm.arsnova.model.Announcement;
import de.thm.arsnova.persistence.AnnouncementRepository;

@Service
@Primary
public class AnnouncementServiceImpl extends DefaultEntityServiceImpl<Announcement> implements AnnouncementService {
	private AnnouncementRepository announcementRepository;
	private UserService userService;

	public AnnouncementServiceImpl(
			final AnnouncementRepository repository,
			@Qualifier("defaultJsonMessageConverter")
			final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
			final Validator validator,
			final UserService userService) {
		super(Announcement.class, repository, jackson2HttpMessageConverter.getObjectMapper(), validator);
		this.announcementRepository = repository;
		this.userService = userService;
	}

	@Override
	public List<Announcement> getByRoomId(final String roomId) {
		final List<String> ids = announcementRepository.findIdsByRoomId(roomId);
		return get(ids);
	}

	@Override
	public List<Announcement> getByRoomIds(final List<String> roomIds) {
		final List<String> ids = announcementRepository.findIdsByRoomIds(roomIds);
		return get(ids);
	}

	@Override
	protected void prepareCreate(final Announcement announcement) {
		final String creatorId = userService.getCurrentUser().getId();
		announcement.setCreatorId(creatorId);
	}
}
