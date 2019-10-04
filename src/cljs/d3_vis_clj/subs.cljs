(ns d3-vis-clj.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]))

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

(rf/reg-sub :sim-set
  (fn [db]
    (:sim db)))

(rf/reg-sub :node-size
  (fn [db]
    (get-in db [:node-config :r])))