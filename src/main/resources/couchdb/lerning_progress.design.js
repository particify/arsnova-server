var designDoc = {
	"_id": "_design/learning_progress",
	"language": "javascript",
	"views": {
		"question_value_achieved_for_user": {
			"comment": "This view returns the points users scored for answered questions.",
			"map": function (doc) {
				if (doc.type === "Answer" && !doc.abstention) {
					/* The 'questionValue' contains the points scored with this answer,
					 * and this could be negative if a wrong answer was given.
					 * However, we do not want negative values, so we set the lower bound to 0.*/
					var score = Math.max(doc.questionValue || 0, 0);
					emit([doc.sessionId, doc.user], {
						questionId: doc.questionId,
						score: score, piRound: doc.piRound
					});
				}
			}
		},
		"maximum_value_of_question": {
			"comment": "This view returns the maximum number that can be achieved when answering this question.",
			"map": function (doc) {
				/* The question's value is determined by the maximum of all possibleAnswer values.
				 * We assume that a correct answer is assigned a positive value,
				 * while a negative value suggests a wrong answer.
				 * The goal then is to get the highest possible value.
				 * This leaves us with two cases:
				 * 1) On any single choice question, the value is the maximum of all possibleAnswer values.
				 * 2) On a multiple choice question, we add up all positive values. */
				 var value = 0, answers = [], positiveAnswers = [], score = 0;
				 if (doc.type === "Content" && ["school", "flashcard"].indexOf(doc.questionType) === -1) {
				 	if ("freetext" === doc.questionType && !doc.fixedAnswer) { return; }
					answers = doc.possibleAnswers.map(function (answer) { return answer.value || 0; });
					/* find the maximum value */
					if (doc.fixedAnswer) { value = doc.rating; }
					else { value = Math.max.apply(null, [0].concat(answers)); }
					/* ignore likert ('vote') questions without any points */
					if (doc.questionType === "vote" && value === 0) { return; }
					/* special case for mc and grid questions: add up all positive answers. */
					if (["grid", "mc"].indexOf(doc.questionType) !== -1) {
						positiveAnswers = answers.filter(function (val) { return val >= 0; });
						if (positiveAnswers.length > 0) {
							value = positiveAnswers.reduce(function (prev, cur) { return prev + cur; }, 0);
						}
					}
					emit([doc.sessionId, doc._id], {
						value: value,
						questionVariant: doc.questionVariant,
						piRound: doc.piRound
					});
				}
			}
		}
	}
};
