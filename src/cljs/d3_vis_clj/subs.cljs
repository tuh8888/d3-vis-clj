(ns d3-vis-clj.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]
            [d3-vis-clj.d3-force :as force]))

(rf/reg-sub :name
  (fn [db]
    (:name db)))

(rf/reg-sub :viz
  (fn [db [_ viz-name]]
    (get db viz-name)))

(rf/reg-sub :window-dims
  (fn [db]
    [(:width db) (:height db)]))

(rf/reg-sub :get-data
  (fn [db [_ viz-name var]]
    (get-in db [viz-name :data var])))

(rf/reg-sub :hierarchy
  (fn [db]
    (get-in db [:all-data :hierarchy])))

(rf/reg-sub :sim
  (fn [db [_ viz-name]]
    (get-in db [viz-name :sim])))

(rf/reg-sub :node-size
  (fn [db [_ viz-name]]
    (get-in db [viz-name :node-config :r])))

(rf/reg-sub :link-config
  (fn [db [_ viz-name]]
    (get-in db [viz-name :link-config])))

(rf/reg-sub :layout-config
  (fn [db [_ viz-name force]]
    (get-in db [viz-name :layout-config force])))

(rf/reg-sub :node-to-add
  (fn [db [_ viz-name]]
    (get-in db [viz-name :node-to-add])))