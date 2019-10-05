(ns d3-vis-clj.db)

(def example-mops
  {:hierarchy (-> (make-hierarchy)
                  (derive :x1 :A)
                  (derive :x2 :B)
                  (derive :x3 :A)
                  (derive :x4 :B))
   :mops {:x1 {:id :x1 :name "a" :slots {:r1 :x2
                                         :r2 :x3
                                         :r3 :x4}}
          :x2 {:id :x2 :name "b" :slots {:r1 :x3
                                         :r2 :x3}}
          :x3 {:id :x3 :name "c"}
          :x4 {:id :x4 :name "d" :slots {:r1 :x1}}}})

(def nodes
  [{:id "mammal" :group 0 :label "Mammals" :level 1}
   {:id "dog" :group 0 :label "Dogs" :level 2}
   {:id "cat" :group 0 :label "Cats" :level 2}
   {:id "fox" :group 0 :label "Foxes" :level 2}
   {:id "elk" :group 0 :label "Elk" :level 2}
   {:id "insect" :group 1 :label "Insects" :level 1}
   {:id "ant" :group 1 :label "Ants" :level 2}
   {:id "bee" :group 1 :label "Bees" :level 2}
   {:id "fish" :group 2 :label "Fish" :level 1}
   {:id "carp" :group 2 :label "Carp" :level 2}
   {:id "pike" :group 2 :label "Pikes" :level 2}])

(def links
  [{:target "mammal" :source "dog" :strength 0.7}
   {:target "mammal" :source "cat" :strength 0.7}
   {:target "mammal" :source "fox" :strength 0.7}
   {:target "mammal" :source "elk" :strength 0.7}
   {:target "insect" :source "ant" :strength 0.7}
   {:target "insect" :source "bee" :strength 0.7}
   {:target "fish" :source "carp" :strength 0.7}
   {:target "fish" :source "pike" :strength 0.7}
   {:target "cat" :source "elk" :strength 0.1}
   {:target "carp" :source "ant" :strength 0.1}
   {:target "elk" :source "bee" :strength 0.1}
   {:target "dog" :source "cat" :strength 0.1}
   {:target "fox" :source "ant" :strength 0.1}
   {:target "pike" :source "dog" :strength 0.1}])

(def default-db
  {:name    "d3-vis-clj"
   :all-data example-mops
   :network {:layout-config {:heat    0.3
                             :collide true
                             :center  true
                             :charge  {:strength -120}
                             :link    {:distance 10
                                       :strength :true}}
             :node-config   {:r    10
                             :fill "blue"}
             :link-config   {:stroke-width 1
                             :stroke       "#E5E5E5"}
             :data          {:nodes []
                             :links []}}})

