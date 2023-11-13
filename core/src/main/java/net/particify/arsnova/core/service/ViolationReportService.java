package net.particify.arsnova.core.service;

import java.util.List;

import net.particify.arsnova.core.model.ViolationReport;

public interface ViolationReportService extends EntityService<ViolationReport> {
  List<ViolationReport> getAllByHasDecisionOrderByCreationTimestampDesc(boolean hasDecision);
}
