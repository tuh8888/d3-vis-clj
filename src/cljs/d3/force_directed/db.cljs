(ns d3.force-directed.db)

(def default-force-layout
  {:type          :d3.force-directed.subs-evts/force-layout
   :layout-config {:heat    0.3
                   :collide true
                   :center  true
                   :charge  {:strength -120}
                   :link    {:distance 10
                             :strength :true}}})