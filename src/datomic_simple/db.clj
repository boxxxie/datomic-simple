(ns datomic-simple.db
  (:require [datomic.api :as d]))
;;; Handles database values and connections

(def ^{:dynamic true :doc "Datomic uri available to all datomic-simple fns."} *uri*)
(def ^{:dynamic true :doc "Datomic connection available to all datomic-simple fns."} *connection*)
(def ^{:dynamic true :doc "Datomic database value available to all datomic-simple fns."} *db*)

; from https://gist.github.com/3150938
(defn wrap-datomic
  "A Ring middleware that provides a request-consistent database connection and
  value for the life of a request."
  [handler uri]
  (fn [request]
    (let [conn (d/connect uri)]
      (binding [*connection* conn
                *db*         (d/db conn)]
        (handler request)))))


(defmacro with-bound-or-latest-database
  "Runs the body with the latest version of that database bound to
  *db*, rather than the request-consistent database."
  [& body]
  `(binding [*connection* (d/connect *uri*)
             *db* (if (bound? #'*db*) *db* (d/db *connection*))]
    ~@body))

; *db* fns - these get overriden by repl-init in a repl
;; (defn q [query & args] (apply d/q query *db* args))
;; (defn entity [id] (d/entity *db* id))
;; (defn resolve-tempid [tempids tempid] (d/resolve-tempid *db* tempids tempid))
(defn q [query & args] (with-bound-or-latest-database (apply d/q query *db* args)))
(defn resolve-tempid [tempids tempid] (with-bound-or-latest-database (d/resolve-tempid *db* tempids tempid)))
(defn entity [id] (with-bound-or-latest-database (d/entity *db* id)))


; TODO: Wrap around fns with dynamic variables without needing to respecify their implementation
(defn repl-init
  "Initializes repl by setting all required dynamic variables and wrapping datomic fns with them."
  [uri]
  (def ^:dynamic *connection* (d/connect uri))
  )

(defn transact
  "Wraps around datomic.api/transact."
  [tx]
  (binding [*connection* (d/connect *uri*)]
    (d/transact *connection* tx)))

; TODO: replace prn with proper logging
(defn transact!
  "Wraps around datomic.api/transact and derefs it."
  [tx]
  (prn "Transacting..." tx)
  @(transact tx))

(defn load-schemas
  "Loads schemas for a given uri."
  [schemas]
  (binding [*connection* (d/connect *uri*)]
    (transact! (flatten schemas))))

(defn add-new-id
  "Adds a tempid to a map of attributes."
  [attr]
  (merge {:db/id (d/tempid :db.part/user)} attr))

(defn load-seed-data
  "Loads seed data for a given uri."
  [uri data]
   (binding [*connection* (d/connect uri)]
    (transact! (map add-new-id (flatten data)))))

(defn set-uri
  "Sets *uri*."
  [uri]
  (def ^:dynamic *uri* uri))
