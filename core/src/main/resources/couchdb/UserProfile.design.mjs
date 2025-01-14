export let designDoc = {
  "_id": "_design/UserProfile",
  "language": "javascript",
  "views": {
    "by_id": {
      "map": function (doc) {
        if (doc.type === "UserProfile") {
          emit(doc._id, {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    },
    "by_creationtimestamp_for_inactive": {
      "map": function (doc) {
        if (doc.type === "UserProfile" && doc.authProvider === "ARSNOVA" && doc.account.activationKey) {
          emit(new Date(doc.creationTimestamp).getTime(), {_rev: doc._rev});
        }
      }
    },
    "by_authprovider_loginid": {
      "map": function (doc) {
        if (doc.type === "UserProfile") {
          emit([doc.authProvider, doc.loginId], {_rev: doc._rev});
        }
      }
    },
    "by_loginid": {
      "map": function (doc) {
        if (doc.type === "UserProfile") {
          emit(doc.loginId, {_rev: doc._rev});
        }
      }
    },
    "by_mail": {
      "map": function (doc) {
        if (doc.type === "UserProfile" && doc.person && doc.person.mail) {
          emit(doc.person.mail, {_rev: doc._rev});
        }
      }
    },
    "by_lastactivitytimestamp": {
      "map": function (doc) {
        if (doc.type === "UserProfile" && doc.lastActivityTimestamp) {
          emit(new Date(doc.lastActivityTimestamp).getTime(), {_rev: doc._rev});
        }
      },
      "reduce": "_count"
    }
  }
};
