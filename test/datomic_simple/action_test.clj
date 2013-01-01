(ns datomic-simple.action-test
  (:use clojure.test
        clojure.contrib.mock)
  (:require [datomic-simple.action :as action]))

(deftest tests-for-actions
  (testing "raw-where using a stub for db/q"
   (expect [db/q (has-args (returns {:db/id 123 :user "fooo" :a "a" :b 6 :c :d}))]
           (is (= {:db/id 123 :user "fooo" :a "a" :b 6 :c :d}
                  (action/raw-where "noise" :noise)))))
  (testing "entity->where"
    (let [entity {:db/id 123 :user "foo" :passhash "yhgd" :email "foo@bar.com"}]
      (is (= entity
             (action/entity->map entity))))))