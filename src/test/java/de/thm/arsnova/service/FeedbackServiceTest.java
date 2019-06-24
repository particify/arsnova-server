package de.thm.arsnova.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import de.thm.arsnova.event.DeleteFeedbackForRoomsEvent;
import de.thm.arsnova.model.Feedback;
import de.thm.arsnova.model.Room;
import de.thm.arsnova.web.exceptions.NoContentException;
import de.thm.arsnova.web.exceptions.NotFoundException;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class FeedbackServiceTest {

	private String roomId;

	private Room room;

	private FeedbackService feedbackService;

	@Mock
	private RoomService roomService;

	@Mock
	private ApplicationEventPublisher publisher;

	@Before
	public void setUp() {
		this.roomId = "1234";
		this.room = new Room();
		room.setId(this.roomId);

		lenient().when(this.roomService.get(anyString())).thenReturn(null);
		lenient().when(this.roomService.get(eq(this.roomId))).thenReturn(room);

		FeedbackStorageService fss = new FeedbackStorageServiceImpl();
		this.feedbackService = new FeedbackServiceImpl(fss, this.roomService);
		((FeedbackServiceImpl) this.feedbackService).setApplicationEventPublisher(this.publisher);
	}

	@Test
	public void shouldCountVotes() {
		feedbackService.save(roomId, 0, "user-id-one");
		feedbackService.save(roomId, 0, "user-id-two");

		int actual = feedbackService.countFeedbackByRoomId(roomId);

		assertEquals(2, actual);
	}

	@Test
	public void shouldCalculateAverageFeedback() {
		feedbackService.save(roomId, 0, "user-id-one");
		feedbackService.save(roomId, 3, "user-id-two");

		double expected = 1.5;
		double actual = feedbackService.calculateAverageFeedback(roomId);

		assertEquals(expected, actual, 0.01);
	}

	@Test(expected = NoContentException.class)
	public void averageCalculationShouldThrowNoContentExceptionWhenNoFeedbackIsPresent() {
		feedbackService.calculateAverageFeedback(roomId);
	}

	@Test(expected = NotFoundException.class)
	public void averageCalculationShouldThrowNotFoundExceptionWhenRoomIsUnknown() {
		feedbackService.calculateAverageFeedback("room-id-does-not-exist");
	}

	@Test
	public void shouldReturnCompleteFeedbackEntity() {
		feedbackService.save(roomId, 0, "user-id-one");
		feedbackService.save(roomId, 3, "user-id-two");

		Feedback expected = new Feedback(1, 0, 0, 1);
		Feedback actual = feedbackService.getByRoomId(roomId);

		assertEquals(expected, actual);
	}

	@Test
	public void shouldReturnSingleVoteFromUser() {
		feedbackService.save(roomId, 2, "user-id-one");

		int actual = feedbackService.getByRoomIdAndUserId(roomId, "user-id-one");

		assertEquals(2, actual);
	}

	@Test
	public void shouldDeleteOldFeedbackVotes() {
		FeedbackStorageService fss = Mockito.mock(FeedbackStorageService.class);
		this.feedbackService = new FeedbackServiceImpl(fss, this.roomService);
		((FeedbackServiceImpl) this.feedbackService).setApplicationEventPublisher(this.publisher);

		Map<Room, List<String>> roomToUserMappings = new HashMap<Room, List<String>>() {{
			put(room, Arrays.asList("user-id-one"));
		}};
		when(fss.cleanVotes(anyInt())).thenReturn(roomToUserMappings);
		feedbackService.save(roomId, 0, "user-id-one");

		feedbackService.cleanFeedbackVotes();

		verify(publisher).publishEvent(argThat(event -> event instanceof DeleteFeedbackForRoomsEvent));
	}

	@Test
	public void shouldDeleteOldFeedbackVotesForSpecificRoom() {
		FeedbackStorageService fss = Mockito.mock(FeedbackStorageService.class);
		this.feedbackService = new FeedbackServiceImpl(fss, this.roomService);
		((FeedbackServiceImpl) this.feedbackService).setApplicationEventPublisher(this.publisher);

		when(fss.cleanVotesByRoom(eq(room), anyInt())).thenReturn(Arrays.asList("user-id-one"));
		feedbackService.save(roomId, 0, "user-id-one");

		feedbackService.cleanFeedbackVotesByRoomId(roomId, 0);

		verify(publisher).publishEvent(argThat(event -> event instanceof DeleteFeedbackForRoomsEvent));
	}
}
