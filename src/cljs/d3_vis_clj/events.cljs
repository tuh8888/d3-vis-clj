(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.db :as db]
            [d3-vis-clj.d3-force :as force]))

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

(rf/reg-event-db :initialize-sim
  (fn [db]
    (let [sim (js/d3.forceSimulation)]
      (-> sim
          (d3-vis-clj.d3-force/set-nodes! (clj->js db/nodes))
          (d3-vis-clj.d3-force/set-forces! (clj->js db/links)))
      (assoc db :sim sim))))

(rf/reg-event-db :add-node
  (fn [db]
    (let [sim (:sim db)
          new-node (hash-map :id "new" :group 0 :label "New node" :level 3)
          new-link (hash-map :target "mammal" :source "new" :strength 0.1)
          nodes (-> sim
                    (force/get-nodes)
                    (js->clj)
                    (conj new-node)
                    (clj->js))
          links (-> sim
                    (force/get-links)
                    (js->clj)
                    (conj new-link)
                    (clj->js))]
      (force/set-nodes! sim nodes)
      (force/set-links! sim links)
      (assoc db :added true))))