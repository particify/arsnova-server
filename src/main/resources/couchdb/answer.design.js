var designDoc = {
	"_id": "_design/answer",
	"language": "javascript",
	"views": {
		"doc_by_questionid_user_piround": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.questionId, doc.user, doc.piRound], doc);
				}
			}
		},
		"doc_by_questionid_timestamp": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.questionId, doc.timestamp], doc);
				}
			}
		},
		"doc_by_user_sessionid": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.user, doc.sessionId], doc);
				}
			}
		},
		"by_questionid": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit(doc.questionId, null);
				}
			}
		},
		"by_questionid_piround_text_subject": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.questionId, doc.piRound, doc.abstention, doc.answerText, doc.answerSubject, doc.successfulFreeTextAnswer], null);
				}
			},
			"reduce": "_count"
		},
		"by_sessionid": {
			/* Redundant view but kept for now to allow simpler queries. */
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit(doc.sessionId, null);
				}
			},
			"reduce": "_count"
		},
		"by_sessionid_variant": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.sessionId, doc.questionVariant], null);
				}
			},
			"reduce": "_count"
		},
		"questionid_by_user_sessionid_variant": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.user, doc.sessionId, doc.questionVariant], doc.questionId);
				}
			}
		},
		"questionid_piround_by_user_sessionid_variant": {
			"map": function (doc) {
				if (doc.type === "skill_question_answer") {
					emit([doc.user, doc.sessionId, doc.questionVariant], [doc.questionId, doc.piRound]);
				}
			}
		}
	}
};
