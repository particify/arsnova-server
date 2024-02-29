export let designDoc = {
  "_id": "_design/Deletion",
  "language": "javascript",
  "views": {
    "by_id": {
      "map": function (doc) {
        if (doc.type === "Deletion") {
          emit(doc._id, {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    },
    "by_entitytype": {
      "map": function (doc) {
        if (doc.type === "Deletion") {
          emit(doc.entityType, {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    }
  }
}
