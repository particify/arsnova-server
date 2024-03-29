export let designDoc = {
  "_id": "_design/ContentGroupTemplate",
  "language": "javascript",
  "views": {
    "by_id": {
      "map": function (doc) {
        if (doc.type === "ContentGroupTemplate") {
          emit(doc._id, {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    }
  }
};
