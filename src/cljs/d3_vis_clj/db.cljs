(ns d3-vis-clj.db)

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

(def default-force-data
  {:data {:nodes [(get-in example-mops [:mops :x1])]}})

(def default-db
  {:name     "d3-vis-clj"
   :all-data example-mops})