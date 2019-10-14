(ns d3-vis-clj.db
  (:require [taoensso.carmine :as car]))

(def example-mops
  {:hierarchy (-> (make-hierarchy)
                  (derive :x1 :A)
                  (derive :x2 :B)
                  (derive :x3 :A)
                  (derive :x4 :B))
   :mops      {:x1 {:id :x1 :name "a" :slots {:r1 #{:x2}
                                              :r2 #{:x3}
                                              :r3 #{:x4}}}
               :x2 {:id :x2 :name "b" :slots {:r1 #{:x3}
                                              :r2 #{:x3}}}
               :x3 {:id :x3 :name "c"}
               :x4 {:id :x4 :name "d" :slots {:r1 #{:x1}}}}})

;;; redis server

(def server-conn {:pool {}
                  :spec {:port 7001}})
(def mops-database 1)
(def h-database 2)

(defmacro wcar* [& body]
  `(car/wcar server-conn ~@body))

(defn init-redis-db
  "Initializes values in the redis db using values supplied here.
  Only needs to be run if the db changes or if the redis db is flushed."
  []
  (for [[id mop] (:mops example-mops)]
    (wcar*
      (car/select mops-database)
      (car/set id mop)))

  (for [[id data] (:hierarchy example-mops)]
    (wcar*
      (car/select h-database)
      (car/set id data))))

(defn get-mop
  [id]
  (second (wcar*
            (car/select mops-database)
            (car/get id))))

(defn select-mops
  [ids]
  (zipmap (map keyword ids)
          (-> (wcar*
                (car/select mops-database)
                (apply car/mget ids))
              (second))))

(defn get-all-db
  [db]
  (let [ids (second (wcar*
                      (car/select db)
                      (car/keys "*")))]
    (zipmap (map keyword ids)
            (-> (wcar*
                  (car/select db)
                  (apply car/mget ids))
                (second)))))

(defn get-hierarchy
  []
  (get-all-db h-database))

(defn get-names
  []
  (let [mops (get-all-db mops-database)]
    mops
    (->> mops
         (vals)
         (map #(select-keys % [:name :id]))
         (zipmap (keys mops)))))