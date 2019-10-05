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
  (fn [db [_ viz-name k v]]
    (assoc-in db [viz-name :data k] v)))

(rf/reg-event-db :resize-nodes
  (fn [db [_ viz-name size]]
    (assoc-in db [viz-name :node-config :r] size)))

(rf/reg-event-db :initialize-sim
  (fn [db [_ viz-name]]
    (let [{{{:keys [nodes links]} :data} viz-name} db]
      (assoc-in db [viz-name :sim] (doto (js/d3.forceSimulation)
                                     (force/set-forces! viz-name)
                                     (force/restart viz-name
                                                    nodes links))))))

(rf/reg-event-db :set-node-to-add
  (fn [db [_ viz-name node-id]]
    (assoc-in db [viz-name :node-to-add] (keyword node-id))))

(rf/reg-event-db :add-node
  (fn [db [_ viz-name node-id]]
    (let [{{{:keys [nodes links]} :data} viz-name} db
          new-node (hash-map :id "new" :group 0 :label "New node" :level 3)
          nodes    (conj nodes new-node)
          #_links    #_(conj links new-link)]
      (force/restart @(rf/subscribe [:sim viz-name]) viz-name nodes links)
      (-> db
          (assoc-in [viz-name :data :nodes] nodes)
          (assoc-in [viz-name :data :links] links)))))

(rf/reg-event-db :add-link
  (fn [db [_ viz-name]]
    (let [{{{:keys [nodes links]} :data} viz-name} db
          new-node (hash-map :id "new" :group 0 :label "New node" :level 3)
          new-link (hash-map :target "mammal" :source "new" :strength 0.1)
          nodes    (conj nodes new-node)
          links    (conj links new-link)]
      (force/restart @(rf/subscribe [:sim viz-name]) viz-name nodes links)
      (-> db
          (assoc-in [viz-name :data :nodes] nodes)
          (assoc-in [viz-name :data :links] links)))))