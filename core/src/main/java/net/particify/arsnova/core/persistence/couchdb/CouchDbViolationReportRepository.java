package net.particify.arsnova.core.persistence.couchdb;

import java.util.List;
import java.util.Map;

import net.particify.arsnova.core.model.ViolationReport;
import net.particify.arsnova.core.persistence.ViolationReportRepository;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

public class CouchDbViolationReportRepository
    extends MangoCouchDbCrudRepository<ViolationReport>
    implements ViolationReportRepository, MangoIndexInitializer {
  private static final String INDEX_NAME = "violation-report-index";

  public CouchDbViolationReportRepository(
      final MangoCouchDbConnector db,
      final boolean createIfNotExists) {
    super(ViolationReport.class, db, "by_id", createIfNotExists);
  }

  @Override
  public void createIndexes() {
    createDefaultIndex();
  }

  public void createDefaultIndex() {
    final List<MangoCouchDbConnector.MangoQuery.Sort> fields = List.of(
        new MangoCouchDbConnector.MangoQuery.Sort("creationTimestamp", true),
        new MangoCouchDbConnector.MangoQuery.Sort("decision", true)
    );
    final Map<String, Object> filterSelector = Map.of(
        "type", type.getSimpleName()
    );
    db.createPartialJsonIndex(INDEX_NAME, fields, filterSelector);
  }

  @Override
  public List<ViolationReport> findAllByHasDecisionOrderByCreationTimestampDesc(final boolean hasDecision) {
    final Map<String, Object> querySelector = Map.of(
        "type", type.getSimpleName(),
        "creationTimestamp", Map.of("$exists", true),
        "decision", Map.of("$exists", hasDecision)
    );
    final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(querySelector);
    query.setIndexDocument(INDEX_NAME);
    return db.query(query, type);
  }
}
