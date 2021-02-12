var designDoc = {
	"_id": "_design/Room",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "Room") {
					emit(doc._id, {_rev: doc._rev});
				}
			},
			"reduce": "_count"
		},
		"by_shortid": {
			"map": function (doc) {
				if (doc.type === "Room") {
					emit(doc.shortId, {_rev: doc._rev});
				}
			}
		},
		"by_ownerid": {
			"map": function (doc) {
				if (doc.type === "Room") {
					emit(doc.ownerId, {_rev: doc._rev});
				}
			}
		},
		"by_moderators_containing_userid": {
			"map": function (doc) {
				if (doc.type === "Room" && doc.moderators) {
					doc.moderators.forEach(function (moderator) {
						emit(moderator.userId, {_rev: doc._rev});
					});
				}
			}
		},
		"partial_by_pool_ownerid_name": {
			"map": function (doc) {
				if (doc.type === "Room") {
					emit([!!doc.poolProperties, doc.ownerId, doc.name], {
						abbreviation: doc.abbreviation,
						shortId: doc.shortId,
						locked: doc.locked,
						courseType: doc.courseType,
						creationTimestamp: doc.creationTimestamp
					});
				}
			}
		}
	}
};
