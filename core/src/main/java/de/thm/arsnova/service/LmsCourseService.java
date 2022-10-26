package de.thm.arsnova.service;

import java.util.List;

import de.thm.arsnova.model.Room;
import de.thm.arsnova.model.UserProfile;
import net.particify.arsnova.connector.model.Course;

public interface LmsCourseService {
	List<Course> getCoursesByUserProfile(UserProfile userProfile);

	List<Room> getCourseRoomsByUserProfile(UserProfile userProfile);
}
