package net.particify.arsnova.core.controller;

import com.fasterxml.jackson.annotation.JsonView;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.particify.arsnova.connector.model.Course;
import net.particify.arsnova.connector.model.UserRole;
import net.particify.arsnova.core.config.properties.SystemProperties;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.UserProfile;
import net.particify.arsnova.core.model.serialization.View;
import net.particify.arsnova.core.service.LmsCourseService;
import net.particify.arsnova.core.service.UserService;

/**
 * Provides access to a user's courses in an LMS such as Moodle.
 */
@RestController
@EntityRequestMapping(LmsCourseController.REQUEST_MAPPING)
@ConditionalOnProperty(
    name =  "enabled",
    prefix = SystemProperties.PREFIX + ".lms-connector"
)
public class LmsCourseController {
  public static final String REQUEST_MAPPING = "/user/{id}/lms";
  public static final String COURSES_MAPPING = "/courses";
  public static final String ROOMS_MAPPING = "/rooms";

  private LmsCourseService lmsCourseService;
  private UserService userService;

  public LmsCourseController(
      final @Qualifier("securedLmsCourseService") LmsCourseService lmsCourseService,
      final @Qualifier("securedUserService") UserService userService) {
    this.lmsCourseService = lmsCourseService;
    this.userService = userService;
  }

  @GetMapping(COURSES_MAPPING)
  public List<LmsCourseMembership> listCourses(
      @PathVariable final String id,
      @RequestParam(value = "sortby", defaultValue = "startdate") final String sortby) {
    final UserProfile userProfile = userService.get(id);
    final List<Course> courses = lmsCourseService.getCoursesByUserProfile(userProfile);

    switch (sortby) {
      case "name":
        Collections.sort(courses, Comparator
            .comparing(Course::getFullname)
            .thenComparing(Course::getStartdate, Comparator.nullsFirst(Comparator.reverseOrder())));
        break;
      case "shortname":
        Collections.sort(courses, Comparator
            .comparing(Course::getShortname)
            .thenComparing(Course::getStartdate, Comparator.nullsFirst(Comparator.reverseOrder())));
        break;
      case "enddate":
        Collections.sort(courses, Comparator
            .comparing(Course::getEnddate, Comparator.nullsFirst(Comparator.reverseOrder()))
            .thenComparing(Course::getFullname));
        break;
      default:
        Collections.sort(courses, Comparator
            .comparing(Course::getStartdate, Comparator.nullsFirst(Comparator.reverseOrder()))
            .thenComparing(Course::getFullname));
        break;
    }

    return courses.stream().map(c -> new LmsCourseMembership(c)).collect(Collectors.toList());
  }

  @GetMapping(ROOMS_MAPPING)
  public final List<Room> listCourseRooms(@PathVariable final String id) {
    final UserProfile userProfile = userService.get(id);
    return lmsCourseService.getCourseRoomsByUserProfile(userProfile);
  }

  @JsonView(View.Public.class)
  private class LmsCourseMembership {
    private String id;
    private String fullname;
    private Instant startdate;
    private UserRole userRole;

    private LmsCourseMembership(final Course course) {
      this.id = course.getId();
      this.fullname = course.getFullname();
      this.startdate = course.getStartdate();
      this.userRole = course.getMembership().getUserrole();
    }

    public String getId() {
      return id;
    }

    public String getFullname() {
      return fullname;
    }

    public Instant getStartdate() {
      return startdate;
    }

    public UserRole getUserRole() {
      return userRole;
    }
  }
}
