package net.particify.arsnova.core.persistence.couchdb;

import java.util.List;
import java.util.Map;
import org.ektorp.ComplexKey;

import net.particify.arsnova.core.model.ContentGroupTemplate;
import net.particify.arsnova.core.persistence.ContentGroupTemplateRepository;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

public class CouchDbContentGroupTemplateRepository
    extends MangoCouchDbCrudRepository<ContentGroupTemplate>
    implements ContentGroupTemplateRepository, MangoIndexInitializer {
  private static final String TAGS_INDEX_NAME = "content-group-template-tags-index";

  public CouchDbContentGroupTemplateRepository(
      final MangoCouchDbConnector db,
      final boolean createIfNotExists) {
    super(ContentGroupTemplate.class, db, "by_id", createIfNotExists);
  }

  @Override
  public void createIndexes() {
    createTagIdsIndex();
    createCreatorIdIndex();
    createLanguageAndCreationTimestampDescIndex();
  }

  private void createTagIdsIndex() {
    final List<MangoCouchDbConnector.MangoQuery.Sort> fields = List.of(
        new MangoCouchDbConnector.MangoQuery.Sort("tagIds", false)
    );
    final Map<String, Object> filterSelector = Map.of(
        "type", type.getSimpleName()
    );
    db.createPartialJsonIndex(TAGS_INDEX_NAME, fields, filterSelector);
  }

  private void createCreatorIdIndex() {
    final List<MangoCouchDbConnector.MangoQuery.Sort> fields = List.of(
      new MangoCouchDbConnector.MangoQuery.Sort("creatorId", false)
    );
    final Map<String, Object> filterSelector = Map.of(
        "type", type.getSimpleName()
    );
    db.createPartialJsonIndex(TAGS_INDEX_NAME, fields, filterSelector);
  }

  private void createLanguageAndCreationTimestampDescIndex() {
    final List<MangoCouchDbConnector.MangoQuery.Sort> fields = List.of(
        new MangoCouchDbConnector.MangoQuery.Sort("language", true),
        new MangoCouchDbConnector.MangoQuery.Sort("creationTimestamp", true)
    );
    final Map<String, Object> filterSelector = Map.of(
        "type", type.getSimpleName(),
        "published", true
    );
    db.createPartialJsonIndex(TAGS_INDEX_NAME, fields, filterSelector);
  }

  @Override
  public List<ContentGroupTemplate> findTopByLanguageOrderByCreationTimestampDesc(
      final String language,
      final int topCount) {
    final Map<String, Object> querySelector = Map.of(
        "type", type.getSimpleName(),
        "language", language,
        "creationTimestamp", Map.of("$exists", true),
        "published", true
    );
    final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(querySelector);
    query.setIndexDocument(TAGS_INDEX_NAME);
    query.setSort(List.of(new MangoCouchDbConnector.MangoQuery.Sort("creationTimestamp", true)));
    query.setLimit(topCount);
    return db.query(query, type);
  }

  @Override
  public List<ContentGroupTemplate> findByTagIds(final List<String> tagIds) {
    final Map<String, Object> querySelector = Map.of(
        "type", type.getSimpleName(),
        "tagIds", Map.of("$all", tagIds),
        "published", true
    );
    final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(querySelector);
    query.setIndexDocument(TAGS_INDEX_NAME);
    return db.query(query, type);
  }

  @Override
  public List<ContentGroupTemplate> findByCreatorId(final String creatorId) {
    final List<ContentGroupTemplate> templates = db.queryView(
        createQuery("by_creatorid_updatetimestamp")
            .startKey(ComplexKey.of(creatorId, ComplexKey.emptyObject()))
            .endKey(ComplexKey.of(creatorId, 0))
            .includeDocs(true)
            .reduce(false)
            .descending(true),
        ContentGroupTemplate.class);

    return templates;
  }
}
