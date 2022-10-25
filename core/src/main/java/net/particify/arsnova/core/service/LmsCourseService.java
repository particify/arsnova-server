package net.particify.arsnova.core.service;

import java.util.List;

import net.particify.arsnova.connector.model.Course;
import net.particify.arsnova.core.model.Room;
import net.particify.arsnova.core.model.UserProfile;

public interface LmsCourseService {
  List<Course> getCoursesByUserProfile(UserProfile userProfile);

  List<Room> getCourseRoomsByUserProfile(UserProfile userProfile);
}
