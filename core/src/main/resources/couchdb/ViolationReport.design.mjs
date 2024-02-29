export let designDoc = {
  "_id": "_design/ViolationReport",
  "language": "javascript",
  "views": {
    "by_id": {
      "map": function (doc) {
        if (doc.type === "ViolationReport") {
          emit(doc._id, {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    }
  }
};
