package net.particify.arsnova.core.persistence.couchdb;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

import net.particify.arsnova.core.model.TemplateTag;
import net.particify.arsnova.core.persistence.TemplateTagRepository;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

public class CouchDbTemplateTagRepository
    extends MangoCouchDbCrudRepository<TemplateTag>
    implements TemplateTagRepository {
  private static final String NAMES_INDEX_NAME = "template-tag-language-name-index";

  public CouchDbTemplateTagRepository(
      final MangoCouchDbConnector db,
      final boolean createIfNotExists) {
    super(TemplateTag.class, db, "by_id", createIfNotExists);
  }

  @PostConstruct
  public void createIndexes() {
    createLanguageNameIndex();
  }

  private void createLanguageNameIndex() {
    final List<MangoCouchDbConnector.MangoQuery.Sort> fields = List.of(
      new MangoCouchDbConnector.MangoQuery.Sort("verified", false),
      new MangoCouchDbConnector.MangoQuery.Sort("language", false),
      new MangoCouchDbConnector.MangoQuery.Sort("name", false)
    );
    final Map<String, Object> filterSelector = Map.of(
        "type", type.getSimpleName()
    );
    db.createPartialJsonIndex(NAMES_INDEX_NAME, fields, filterSelector);
  }

  @Override
  public List<TemplateTag> findAllByVerifiedAndLanguage(final boolean verified, final String language) {
    final Map<String, Object> querySelector = Map.of(
        "type", type.getSimpleName(),
        "verified", verified,
        "language", language,
        "name", Map.of("$exists", true)
    );
    final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(querySelector);
    query.setIndexDocument(NAMES_INDEX_NAME);
    return db.query(query, type);
  }

  @Override
  public List<TemplateTag> findAllByNameAndLanguage(final List<String> names, final String language) {
    final Map<String, Object> querySelector = Map.of(
        "type", type.getSimpleName(),
        "verified", Map.of("$exists", true),
        "language", language,
        "name", Map.of("$in", names)
    );
    final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(querySelector);
    query.setIndexDocument(NAMES_INDEX_NAME);
    return db.query(query, type);
  }
}
