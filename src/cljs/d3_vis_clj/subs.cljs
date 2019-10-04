(ns d3-vis-clj.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]
            [d3-vis-clj.d3-force :as force]))

(rf/reg-sub :name
  (fn [db]
    (:name db)))

(rf/reg-sub :global-width
  (fn [db _]
    (:width db)))

(rf/reg-sub :db
  (fn [db]
    db))

(rf/reg-sub :get-data
  (fn [db [_ var]]
    (get-in db [:data var])))

(rf/reg-sub :sim-node
  (fn [db [_ i]]
    (force/sim-node (:sim db) i)))

(rf/reg-sub :sim
  (fn [db]
    (:sim db)))

(rf/reg-sub :node-size
  (fn [db]
    (get-in db [:node-config :r])))