var designDoc = {
	"_id": "_design/statistics",
	"language": "javascript",
	"views": {
		"active_student_users": {
			"map": function (doc) {
				if (doc.type === "Answer") {
					emit(doc.user, 1);
				}
			},
			"reduce": "_count"
		},
		"statistics": {
			"map": function (doc) {
				switch (doc.type) {
				case "Room":
					if (doc.active) {
						emit("openSessions", 1);
					} else {
						emit("closedSessions", 1);
					}
					break;
				case "Content":
					if (doc.questionType === "flashcard") {
						emit("flashcards", 1);
					} else {
						if (doc.questionVariant === "lecture") {
							emit("lectureQuestions", 1);
						} else if (doc.questionVariant === "preparation") {
							emit("preparationQuestions", 1);
						}
						if (doc.piRound === 2) {
							emit("conceptQuestions", 1);
						}
					}
					break;
				case "Answer":
					emit("answers", 1);
					break;
				case "Comment":
					emit ("interposedQuestions", 1);
					break;
				case "LogEntry":
					if (doc.event === "delete") {
						switch (doc.payload.type) {
						case "session":
							emit("deletedSessions", doc.payload.sessionCount || 1);
							break;
						case "question":
							emit("deletedQuestions", doc.payload.questionCount || 1);
							break;
						case "answer":
							emit("deletedAnswers", doc.payload.answerCount || 1);
							break;
						case "comment":
							emit("deletedComments", doc.payload.commentCount || 1);
							break;
						case "user":
							emit("deletedUsers", 1);
							break;
						}
					}
					break;
				}
			},
			"reduce": "_sum"
		},
		"unique_session_creators": {
			"map": function (doc) {
				if (doc.type === "Room") {
					emit(doc.creator, 1);
				}
			},
			"reduce": "_count"
		}
	}
};
