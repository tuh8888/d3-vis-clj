(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.db :as db]))

(rf/reg-event-db :initialize-db
 (fn  [_ _]
   db/default-db))

(rf/reg-event-db :window-resize
  (fn [db _]
    (-> db
        (assoc-in [:height] js/window.innerHeight)
        (assoc-in [:width] js/window.innerWidth))))

(rf/reg-event-db :set-data
  (fn [db [_ k v]]
    (assoc-in db [:data k] v)))

(rf/reg-event-db :resize-nodes
  (fn [db [_ size]]
    (assoc-in db [:node-config :r] size)))

(rf/reg-event-db :add-node
  (fn [db [_ sim]]
    (let [new-node (hash-map :id "new" :group 0 :label "New node" :level 3)]
      (d3-vis-clj.d3-force/sim-nodes! sim (-> sim
                                              (d3-vis-clj.d3-force/sim-nodes)
                                              (js->clj)
                                              (conj new-node)
                                              (clj->js)))

      (-> db
          (update-in [:data :nodes] #(conj % new-node))
          (update-in [:data :links] #(conj % (hash-map :target "mammal" :source "new" :strength 0.1)))))))