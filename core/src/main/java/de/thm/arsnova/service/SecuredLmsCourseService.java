package de.thm.arsnova.service;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import net.particify.arsnova.connector.model.Course;

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
