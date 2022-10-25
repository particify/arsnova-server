package net.particify.arsnova.core.service;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import net.particify.arsnova.connector.model.Course;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.UserProfile;

@Service
@ConditionalOnProperty(
    name =  "enabled",
    prefix = SystemProperties.PREFIX + ".lms-connector"
)
public class SecuredLmsCourseService implements LmsCourseService {
  private LmsCourseService lmsCourseService;

  public SecuredLmsCourseService(final LmsCourseService lmsCourseService) {
    this.lmsCourseService = lmsCourseService;
  }

  @Override
  @PreAuthorize("hasPermission(#userProfile, 'owner')")
  public List<Course> getCoursesByUserProfile(final UserProfile userProfile) {
    return lmsCourseService.getCoursesByUserProfile(userProfile);
  }

  @Override
  @PreAuthorize("hasPermission(#userProfile, 'owner')")
  public List<Room> getCourseRoomsByUserProfile(final UserProfile userProfile) {
    return lmsCourseService.getCourseRoomsByUserProfile(userProfile);
  }
}
