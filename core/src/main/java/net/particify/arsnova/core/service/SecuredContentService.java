package net.particify.arsnova.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.Content;
import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.model.ContentTemplate;
import net.particify.arsnova.core.model.WordContent;

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
  @PostFilter(value = "hasPermission(filterObject, 'content', 'read')")
  public List<Content> getByRoomId(final String roomId) {
    return contentService.getByRoomId(roomId);
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'read')")
  public int countByRoomId(final String roomId) {
    return contentService.countByRoomId(roomId);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read-correct-options')")
  public List<Integer> getCorrectChoiceIndexes(final String contentId) {
    return contentService.getCorrectChoiceIndexes(contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'read-correct-options')")
  public Set<String> getCorrectTerms(final String contentId) {
    return contentService.getCorrectTerms(contentId);
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
  @PreAuthorize("hasPermission(#wordContent, 'moderate')")
  public void addToBannedKeywords(final WordContent wordContent, final String keyword) {
    contentService.addToBannedKeywords(wordContent, keyword);
  }

  @Override
  @PreAuthorize("hasPermission(#wordContent, 'moderate')")
  public void clearBannedKeywords(final WordContent wordContent) {
    contentService.clearBannedKeywords(wordContent);
  }

  @Override
  @PreAuthorize("hasPermission(#roomId, 'room', 'update')")
  public List<Content> createFromTemplates(
      final String roomId,
      final ContentGroupTemplate contentGroupTemplate,
      final List<ContentTemplate> templates) {
    return contentService.createFromTemplates(roomId, contentGroupTemplate, templates);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'update')")
  public void stop(final String contentId) {
    contentService.stop(contentId);
  }

  @Override
  @PreAuthorize("hasPermission(#contentId, 'content', 'update')")
  public void startRound(final String contentId, final int round) {
    contentService.startRound(contentId, round);
  }
}
