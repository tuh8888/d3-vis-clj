(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.d3-force :as force]
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

(rf/reg-event-db :set-sim
  (fn [db [_ sim]]
    (assoc db :sim sim)))

(rf/reg-event-db :resize-nodes
  (fn [db [_ size]]
    (assoc-in db [:node-config :r] size)))



