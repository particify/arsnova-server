var designDoc = {
	"_id": "_design/LoggedIn",
	"language": "javascript",
	"views": {
		"all": {
			"map": function (doc) {
				if (doc.type === "logged_in"){
					emit(doc.user, doc);
				}
			}
		},
		"by_last_activity_for_guests": {
			"map": function (doc) {
				if (doc.type === "logged_in" && doc.user.indexOf("Guest") === 0) {
					emit(doc.timestamp || 0, {_rev: doc._rev});
				}
			}
		},
		"visited_sessions_by_user": {
			"map": function (doc) {
				if (doc.type === "logged_in") {
					emit(doc.user, doc.visitedSessions);
				}
			}
		}
	}
};
