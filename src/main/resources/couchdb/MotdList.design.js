var designDoc = {
	"_id": "_design/MotdList",
	"language": "javascript",
	"views": {
		"doc_by_username": {
			"map": function (doc) {
				if (doc.type === "motdlist") {
					emit(doc.username, doc);
				}
			}
		}
	}
};
