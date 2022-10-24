package de.thm.arsnova.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import de.thm.arsnova.config.properties.SystemProperties;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import de.thm.arsnova.persistence.RoomRepository;
import net.particify.arsnova.connector.client.ConnectorClient;
import net.particify.arsnova.connector.model.Course;
import net.particify.arsnova.connector.model.UserRole;

@Service
@Primary
@ConditionalOnProperty(
    name =  "enabled",
    prefix = SystemProperties.PREFIX + ".lms-connector"
)
public class LmsCourseServiceImpl implements LmsCourseService {
  private ConnectorClient connectorClient;
  private RoomService roomService;
  private RoomRepository roomRepository;

  public LmsCourseServiceImpl(
      final ConnectorClient connectorClient,
      final RoomService roomService,
      final RoomRepository roomRepository) {
    this.connectorClient = connectorClient;
    this.roomService = roomService;
    this.roomRepository = roomRepository;
  }

  @Override
  public List<Course> getCoursesByUserProfile(final UserProfile userProfile) {
    final List<Course> courses = new ArrayList<>();

    for (final Course course : connectorClient.getCourses(userProfile.getLoginId()).getCourse()) {
      if (course.getMembership().isMember()
          && course.getMembership().getUserrole().equals(UserRole.TEACHER)
      ) {
        courses.add(course);
      }
    }

    return courses;
  }

  @Override
  public List<Room> getCourseRoomsByUserProfile(final UserProfile userProfile) {
    final List<String> courseIds = connectorClient.getCourses(userProfile.getLoginId()).getCourse().stream()
        .map(c -> c.getId())
        .collect(Collectors.toList());
    return roomService.get(roomRepository.findIdsByLmsCourseIds(courseIds));
  }
}
