(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.db :as db]
            [d3-vis-clj.d3-force :as force]))

(rf/reg-event-db :initialize-db
  (fn [_ _]
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
  (fn [{:as db
        {:keys [nodes links]} :data}]
    (assoc db :sim (doto (js/d3.forceSimulation)
                     (force/set-forces!)
                     (force/restart nodes
                                    links)))))

(rf/reg-event-db :add-node
  (fn [{:as db
        {:keys [nodes links]} :data}]
    (let [new-node (hash-map :id "new" :group 0 :label "New node" :level 3)
          new-link (hash-map :target "mammal" :source "new" :strength 0.1)
          nodes (conj nodes new-node)
          links (conj links new-link)]
      (force/restart (:sim db) nodes links)
      (-> db
          (assoc-in [:data :nodes] nodes)
          (assoc-in [:data :links] links)))))