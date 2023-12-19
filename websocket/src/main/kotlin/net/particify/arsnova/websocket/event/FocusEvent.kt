package net.particify.arsnova.websocket.event

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.EXISTING_PROPERTY,
  property = "feature",
)
@JsonSubTypes(
  JsonSubTypes.Type(ContentFocusEvent::class, name = "CONTENTS"),
  JsonSubTypes.Type(CommentFocusEvent::class, name = "COMMENTS"),
  JsonSubTypes.Type(FeedbackFocusEvent::class, name = "FEEDBACK"),
  JsonSubTypes.Type(OverviewFocusEvent::class, name = "OVERVIEW"),
)
open abstract class FocusEvent(
  val feature: FocusView,
)

enum class FocusView {
  CONTENTS,
  COMMENTS,
  FEEDBACK,
  OVERVIEW,
}

class OverviewFocusEvent() : FocusEvent(FocusView.OVERVIEW)

data class ContentFocusEvent(
  val focusState: ContentFocusState,
) : FocusEvent(FocusView.CONTENTS)

data class ContentFocusState(
  val contentId: String,
  val contentIndex: Int,
  val contentGroupId: String,
  val contentGroupName: String,
)

data class CommentFocusEvent(
  val focusState: CommentFocusState,
) : FocusEvent(FocusView.COMMENTS)

data class CommentFocusState(
  val commentId: String,
)

data class FeedbackFocusState(
  val started: Boolean,
)

data class FeedbackFocusEvent(
  val focusState: FeedbackFocusState,
) : FocusEvent(FocusView.FEEDBACK)
