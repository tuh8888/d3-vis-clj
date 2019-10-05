(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.db :as db]
            [d3.force-directed.layout :as layout]))

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
      (assoc-in db [viz-name :sim] (layout/new-sim viz-name nodes links)))))

(rf/reg-event-db :set-node-to-add
  (fn [db [_ viz-name node-id]]
    (assoc-in db [viz-name :node-to-add] (keyword node-id))))

(rf/reg-event-db :add-node
  (fn [db [_ viz-name]]
    (if-let [new-node (get-in db [:all-data :mops @(rf/subscribe [:node-to-add :network])])]
      (let [{{{:keys [nodes links]} :data} viz-name} db
            nodes    (conj nodes new-node)]
        (println nodes)
        (layout/restart @(rf/subscribe [:sim viz-name]) viz-name nodes links)
        (-> db
            (assoc-in [viz-name :data :nodes] nodes)
            (assoc-in [viz-name :data :links] links)))
      db)))