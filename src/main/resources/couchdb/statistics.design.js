var designDoc = {
	"_id": "_design/statistics",
	"language": "javascript",
	"views": {
		"statistics": {
			"map": function (doc) {
				emit([doc.type], 1);
				switch (doc.type) {
				case "UserProfile":
					emit([doc.type, "authProvider", doc.authProvider], 1);
					if (doc.account && doc.account.activationKey) {
						emit([doc.type, "activationPending"], 1);
					}
					break;
				case "Room":
					if (doc.closed) {
						emit([doc.type, "closed"], 1);
					}
					break;
				case "Content":
					emit([doc.type, "format", doc.format], 1);
					break;
				case "Answer":
					emit([doc.type, "format", doc.format], 1);
					break;
				case "LogEntry":
					if (doc.event === "delete") {
						switch (doc.payload.type) {
						case "UserProfile":
							emit([doc.payload.type, "deleted"], 1);
							break;
						case "Room":
							emit([doc.payload.type, "deleted"], doc.payload.roomCount || 1);
							break;
						case "Content":
							emit([doc.payload.type, "deleted"], doc.payload.contentCount || 1);
							break;
						case "Answer":
							emit([doc.payload.type, "deleted"], doc.payload.answerCount || 1);
							break;
						}
					}
					break;
				}
			},
			"reduce": "_sum"
		},
		"unique_room_owners": {
			"map": function (doc) {
				if (doc.type === "Room") {
					emit(doc.ownerId, 1);
				}
			},
			"reduce": "_count"
		}
	}
};
