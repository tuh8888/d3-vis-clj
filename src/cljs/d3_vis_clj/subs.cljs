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

(rf/reg-sub :get-node
  (fn [db [_ viz-name i]]
    (get-in db [viz-name :data :nodes i])))

(rf/reg-sub :node-color
  (fn [[_ viz-name i] _]
    (rf/subscribe [:get-node viz-name i]))
  (fn [{:keys [id hovered]} _]
    (cond hovered "yellow"
          (isa? @(rf/subscribe [:hierarchy]) id :A) "red"
          (isa? @(rf/subscribe [:hierarchy]) id :B) "blue"
          :default "green")))

(rf/reg-sub :node-name
  (fn [[_ viz-name i] _]
    (rf/subscribe [:get-node viz-name i]))
  (fn [{:keys [name]} _]
    name))

(rf/reg-sub :drag-fn
  (fn [db [_ viz-name]]
    (get-in db [viz-name :drag] #())))

(rf/reg-sub :get-nodes
  (fn [db [_ viz-name]]
    (get-in db [viz-name :data :nodes])))

(rf/reg-sub :get-nodes-js
  (fn [[_ viz-name] _]
    (rf/subscribe [:get-nodes viz-name]))
  (fn [data _]
    (clj->js data)))

(rf/reg-sub :get-links
  (fn [db [_ viz-name]]
    (get-in db [viz-name :data :links])))

(rf/reg-sub :get-links-js
  (fn [[_ viz-name] _]
    (rf/subscribe [:get-links viz-name]))
  (fn [data _]
    (clj->js data)))