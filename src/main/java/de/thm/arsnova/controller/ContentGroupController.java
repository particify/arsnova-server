package de.thm.arsnova.controller;

import com.fasterxml.jackson.annotation.JsonView;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import de.thm.arsnova.model.ContentGroup;
import de.thm.arsnova.model.serialization.View;
import de.thm.arsnova.service.ContentGroupService;

@RestController
@EntityRequestMapping(ContentGroupController.REQUEST_MAPPING)
public class ContentGroupController extends AbstractEntityController<ContentGroup> {
	protected static final String REQUEST_MAPPING = "/contentgroup";
	private static final String ADD_CONTENT_MAPPING = "/-/content";
	private static final String REMOVE_CONTENT_MAPPING = DEFAULT_ID_MAPPING + "/content/{contentId}";
	private static final String IMPORT_MAPPING = DEFAULT_ID_MAPPING + "/import";

	private ContentGroupService contentGroupService;

	public ContentGroupController(
			@Qualifier("securedContentGroupService") final ContentGroupService contentGroupService) {
		super(contentGroupService);
		this.contentGroupService = contentGroupService;
	}

	@Override
	protected String getMapping() {
		return REQUEST_MAPPING;
	}

	@Override
	public ContentGroup post(
			@RequestBody final ContentGroup entity,
			final HttpServletResponse httpServletResponse) {
		contentGroupService.createOrUpdateContentGroup(entity);
		final String uri = UriComponentsBuilder.fromPath(getMapping()).path(GET_MAPPING)
				.buildAndExpand(entity.getRoomId(), entity.getName()).toUriString();
		httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
		httpServletResponse.setHeader(ENTITY_ID_HEADER, entity.getId());
		httpServletResponse.setHeader(ENTITY_REVISION_HEADER, entity.getRevision());

		return entity;
	}

	@Override
	public ContentGroup put(
			@RequestBody final ContentGroup contentGroup,
			final HttpServletResponse httpServletResponse) {
		final ContentGroup updatedContentGroup = contentGroupService.createOrUpdateContentGroup(contentGroup);

		if (updatedContentGroup.getId() != null) {
			final String uri = UriComponentsBuilder.fromPath(getMapping()).path(GET_MAPPING)
					.buildAndExpand(contentGroup.getRoomId(), contentGroup.getName()).toUriString();
			httpServletResponse.setHeader(HttpHeaders.LOCATION, uri);
			httpServletResponse.setHeader(ENTITY_ID_HEADER, contentGroup.getId());
			httpServletResponse.setHeader(ENTITY_REVISION_HEADER, contentGroup.getRevision());
		}

		return updatedContentGroup;
	}

	@PostMapping(ADD_CONTENT_MAPPING)
	public void addContentToGroup(@RequestBody final AddContentToGroupRequestEntity addContentToGroupRequestEntity) {
		contentGroupService.addContentToGroup(
				addContentToGroupRequestEntity.getRoomId(),
				addContentToGroupRequestEntity.getContentGroupName(),
				addContentToGroupRequestEntity.getContentId());
	}

	@DeleteMapping(REMOVE_CONTENT_MAPPING)
	public void removeContentFromGroup(@PathVariable final String id, @PathVariable final String contentId) {
		contentGroupService.removeContentFromGroup(id, contentId);
	}

	@PostMapping(IMPORT_MAPPING)
	public void importFromFile(@PathVariable final String id, @RequestParam final MultipartFile file)
			throws IOException {
		final ContentGroup contentGroup = get(id);
		contentGroupService.importFromCsv(file.getBytes(), contentGroup);
	}

	static class AddContentToGroupRequestEntity {
		private String roomId;
		private String contentGroupName;
		private String contentId;

		public String getRoomId() {
			return roomId;
		}

		@JsonView(View.Public.class)
		public void setRoomId(final String roomId) {
			this.roomId = roomId;
		}

		public String getContentGroupName() {
			return contentGroupName;
		}

		@JsonView(View.Public.class)
		public void setContentGroupName(final String contentGroupName) {
			this.contentGroupName = contentGroupName;
		}

		public String getContentId() {
			return contentId;
		}

		@JsonView(View.Public.class)
		public void setContentId(final String contentId) {
			this.contentId = contentId;
		}
	}
}
