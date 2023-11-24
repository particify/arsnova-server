package net.particify.arsnova.core.persistence;

import java.util.List;

import net.particify.arsnova.core.model.ViolationReport;

public interface ViolationReportRepository extends CrudRepository<ViolationReport, String> {
  List<ViolationReport> findAllByHasDecisionOrderByCreationTimestampDesc(boolean hasDecision);
}
