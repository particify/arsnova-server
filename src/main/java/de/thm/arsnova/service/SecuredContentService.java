package de.thm.arsnova.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Service;

import de.thm.arsnova.model.Content;
import de.thm.arsnova.model.WordcloudContent;

@Service
public class SecuredContentService extends AbstractSecuredEntityServiceImpl<Content>
		implements ContentService, SecuredService {
	private final ContentService contentService;

	public SecuredContentService(final ContentService contentService) {
		super(Content.class, contentService);
		this.contentService = contentService;
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public List<Content> getByRoomId(final String roomId) {
		return contentService.getByRoomId(roomId);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public Iterable<Content> getByRoomIdAndGroup(final String roomId, final String group) {
		return contentService.getByRoomIdAndGroup(roomId, group);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public int countByRoomId(final String roomId) {
		return contentService.countByRoomId(roomId);
	}

	@Override
	@PreAuthorize("hasPermission(#roomId, 'room', 'read')")
	public int countByRoomIdAndGroup(final String roomId, final String group) {
		return contentService.countByRoomIdAndGroup(roomId, group);
	}

	@Override
	@PreAuthorize("hasPermission(#contentId, 'content', 'read-correct-options')")
	public List<Integer> getCorrectChoiceIndexes(final String contentId) {
		return contentService.getCorrectChoiceIndexes(contentId);
	}

	@Override
	@PreFilter(value = "hasPermission(filterObject, 'content', 'owner')", filterTarget = "contentIds")
	public byte[] exportToCsv(final List<String> contentIds, final String charset) throws JsonProcessingException {
		return contentService.exportToCsv(contentIds, charset);
	}

	@Override
	@PreFilter(value = "hasPermission(filterObject, 'content', 'owner')", filterTarget = "contentIds")
	public byte[] exportToTsv(final List<String> contentIds, final String charset) throws JsonProcessingException {
		return contentService.exportToTsv(contentIds, charset);
	}

	@Override
	@PreAuthorize("hasPermission(#wordcloudContent, 'moderate')")
	public void addToBannedKeywords(final WordcloudContent wordcloudContent, final String keyword) {
		contentService.addToBannedKeywords(wordcloudContent, keyword);
	}

	@Override
	@PreAuthorize("hasPermission(#wordcloudContent, 'moderate')")
	public void clearBannedKeywords(final WordcloudContent wordcloudContent) {
		contentService.clearBannedKeywords(wordcloudContent);
	}
}
