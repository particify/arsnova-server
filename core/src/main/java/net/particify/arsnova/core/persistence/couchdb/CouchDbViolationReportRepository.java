package net.particify.arsnova.core.persistence.couchdb;

import java.util.List;
import java.util.Map;

import net.particify.arsnova.core.model.ViolationReport;
import net.particify.arsnova.core.persistence.ViolationReportRepository;
import net.particify.arsnova.core.persistence.couchdb.support.MangoCouchDbConnector;

public class CouchDbViolationReportRepository
    extends MangoCouchDbCrudRepository<ViolationReport>
    implements ViolationReportRepository, MangoIndexInitializer {
  private static final String WITH_DECISION_INDEX_NAME = "violation-report-w-decision-index";
  private static final String WITHOUT_DECISION_INDEX_NAME = "violation-report-wo-decision-index";

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
        new MangoCouchDbConnector.MangoQuery.Sort("creationTimestamp", true)
    );
    final Map<String, Object> withDecisionFilterSelector = Map.of(
        "type", type.getSimpleName(),
        "decision", Map.of("$exists", true)
    );
    final Map<String, Object> withoutDecisionFilterSelector = Map.of(
        "type", type.getSimpleName(),
        "decision", Map.of("$exists", false)
    );
    db.createPartialJsonIndex(WITH_DECISION_INDEX_NAME, fields, withDecisionFilterSelector);
    db.createPartialJsonIndex(WITHOUT_DECISION_INDEX_NAME, fields, withoutDecisionFilterSelector);
  }

  @Override
  public List<ViolationReport> findAllByHasDecisionOrderByCreationTimestampDesc(final boolean hasDecision) {
    final Map<String, Object> querySelector = Map.of(
        "type", type.getSimpleName(),
        "decision", Map.of("$exists", hasDecision),
        "creationTimestamp", Map.of("$exists", true)
    );
    final MangoCouchDbConnector.MangoQuery query = new MangoCouchDbConnector.MangoQuery(querySelector);
    query.setIndexDocument(hasDecision ? WITH_DECISION_INDEX_NAME : WITHOUT_DECISION_INDEX_NAME);
    return db.query(query, type);
  }
}
