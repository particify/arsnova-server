package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.validation.Validator;

import net.particify.arsnova.core.model.ViolationReport;
import net.particify.arsnova.core.persistence.DeletionRepository;
import net.particify.arsnova.core.persistence.ViolationReportRepository;
import net.particify.arsnova.core.security.AuthenticationService;

@Service
@Primary
public class ViolationReportServiceImpl
    extends DefaultEntityServiceImpl<ViolationReport>
    implements ViolationReportService {
  private ViolationReportRepository violationReportRepository;
  private AuthenticationService authenticationService;

  public ViolationReportServiceImpl(
      final ViolationReportRepository repository,
      final DeletionRepository deletionRepository,
      final AuthenticationService authenticationService,
      @Qualifier("defaultJsonMessageConverter")
      final MappingJackson2HttpMessageConverter jackson2HttpMessageConverter,
      final Validator validator) {
    super(
        ViolationReport.class,
        repository,
        deletionRepository,
        jackson2HttpMessageConverter.getObjectMapper(),
        validator);
    this.violationReportRepository = repository;
    this.authenticationService = authenticationService;
  }

  @Override
  public List<ViolationReport> getAllByHasDecisionOrderByCreationTimestampDesc(final boolean hasDecision) {
    return violationReportRepository.findAllByHasDecisionOrderByCreationTimestampDesc(hasDecision);
  }

  @Override
  public ViolationReport create(final ViolationReport entity) {
    entity.setCreatorId(authenticationService.getCurrentUser().getId());
    entity.setDecision(null);
    return super.create(entity);
  }
}
