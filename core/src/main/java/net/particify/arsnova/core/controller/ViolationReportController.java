package net.particify.arsnova.core.controller;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.core.model.ViolationReport;
import net.particify.arsnova.core.service.ViolationReportService;
import net.particify.arsnova.core.web.exceptions.BadRequestException;

@RestController
@EntityRequestMapping(ViolationReportController.REQUEST_MAPPING)
public class ViolationReportController extends AbstractEntityController<ViolationReport> {
  public static final String REQUEST_MAPPING = "/violationreport";

  private ViolationReportService violationReportService;

  protected ViolationReportController(
      @Qualifier("securedViolationReportService") final ViolationReportService violationReportService) {
    super(violationReportService);
    this.violationReportService = violationReportService;
  }

  @GetMapping(value = DEFAULT_ROOT_MAPPING, params = "hasDecision")
  public List<ViolationReport> getList(@RequestParam final boolean hasDecision) {
    return violationReportService.getAllByHasDecisionOrderByCreationTimestampDesc(hasDecision);
  }

  @Override
  @PostMapping(POST_MAPPING)
  public ViolationReport post(
      @RequestBody final ViolationReport violationReport,
      final HttpServletResponse httpServletResponse) {
    if (violationReport.getTargetType() == null
        || (!violationReport.getTargetType().equals("ContentGroupTemplate")
        && !violationReport.getTargetType().equals("Room"))) {
      throw new BadRequestException("Invalid target type.");
    }
    return violationReportService.create(violationReport);
  }

  @Override
  protected String getMapping() {
    return REQUEST_MAPPING;
  }
}
