(ns d3-vis-clj.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]))

(rf/reg-sub :name
  (fn [db]
    (:name db)))

(rf/reg-sub :window-dims
  (fn [db]
    [(:width db) (:height db)]))

(rf/reg-sub :hierarchy
  (fn [db]
    (get-in db [:all-data :hierarchy])))

(rf/reg-sub :force-layout
  (fn [db [_ viz-name]]
    (get db viz-name)))

(rf/reg-sub :node-size
  (fn [db [_ viz-name]]
    (get-in db [viz-name :node-config :r])))

(rf/reg-sub :node-to-add
  (fn [db [_ viz-name]]
    (get-in db [viz-name :node-to-add])))

(rf/reg-sub :link-config
  (fn [db [_ viz-name]]
    (get-in db [viz-name :link-config])))