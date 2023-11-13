package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.core.model.ViolationReport;

@Service
public class SecuredViolationReportService
    extends AbstractSecuredEntityServiceImpl<ViolationReport>
    implements ViolationReportService, SecuredService {
  private ViolationReportService violationReportService;

  public SecuredViolationReportService(final ViolationReportService violationReportService) {
    super(ViolationReport.class, violationReportService);
    this.violationReportService = violationReportService;
  }

  @Override
  @PreAuthorize(
      "isAuthenticated and !hasRole('GUEST_USER') and hasPermission(#entity.targetId, #entity.targetType, 'read')")
  public ViolationReport create(final ViolationReport entity) {
    return violationReportService.create(entity);
  }

  @Override
  @PreAuthorize("hasRole('ADMIN')")
  public List<ViolationReport> getAllByHasDecisionOrderByCreationTimestampDesc(final boolean hasDecision) {
    return violationReportService.getAllByHasDecisionOrderByCreationTimestampDesc(hasDecision);
  }
}
