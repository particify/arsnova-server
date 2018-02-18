var designDoc = {
	"_id": "_design/Room",
	"language": "javascript",
	"views": {
		"by_id": {
			"map": function (doc) {
				if (doc.type === "Room") {
					emit(doc._id, {_rev: doc._rev});
				}
			}
		},
		"by_courseid": {
			"map": function (doc) {
				if (doc.type === "Room" && doc.courseId  && !doc.poolProperties) {
					emit(doc.courseId, {_rev: doc._rev});
				}
			}
		},
		"by_shortid": {
			"map": function (doc) {
				if (doc.type === "Room") {
					emit(doc.shortId, {_rev: doc._rev});
				}
			}
		},
		"by_lastactivity_for_guests": { /* needs rewrite */
			"map": function (doc) {
				if (doc.type === "Room" && !doc.poolProperties && doc.creator.indexOf("Guest") === 0) {
					emit(doc.lastOwnerActivity || doc.creationTimestamp, {_rev: doc._rev});
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
		},
		"partial_by_category_name_for_pool": {
			"map": function (doc) {
				if (doc.type === "Room" && doc.poolProperties) {
					emit([doc.poolProperties.category, doc.name], {
						name: doc.name,
						shortId: doc.shortId,
						poolProperties: doc.poolProperties
					});
				}
			}
		}
	}
};
