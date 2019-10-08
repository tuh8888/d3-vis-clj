(ns d3.force-directed.subs
  (:require [re-frame.core :refer [subscribe reg-sub]]))

(reg-sub ::node-size
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-config :r])))

(reg-sub ::node-to-add
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-to-add])))

(reg-sub ::link-config
  (fn [db [_ viz-id]]
    (get-in db [viz-id :link-config])))

(reg-sub ::get-node
  (fn [db [_ viz-id i]]
    (get-in db [viz-id :data :nodes i])))

(reg-sub ::get-link
  (fn [db [_ viz-id i]]
    (get-in db [viz-id :data :links i])))

(reg-sub ::selected-node
  (fn [db [_ viz-id]]
    (get-in db [viz-id :selected :node])))

(reg-sub ::node-outer-color
  (fn [[_ viz-id i] _]
    [(subscribe [::selected-node viz-id]) (subscribe [::get-node viz-id i])])
  (fn [[selected-node node] _]
    (if (= (:id selected-node) (:id node))
      "black"
      "white")))

(reg-sub ::node-name
  (fn [[_ viz-id i] _]
    (subscribe [::get-node viz-id i]))
  (fn [{:keys [name]} _]
    name))

(reg-sub ::link-name
  (fn [[_ viz-id i] _]
    (subscribe [::get-link viz-id i]))
  (fn [{:keys [label]}]
    label))

(reg-sub ::drag-fn
  (fn [db [_ viz-id]]
    (get-in db [viz-id :drag] #())))

(reg-sub ::get-nodes
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data :nodes])))

(reg-sub ::get-nodes-js
  (fn [[_ viz-id] _]
    (subscribe [::get-nodes viz-id]))
  (fn [data _]
    (clj->js data)))

(reg-sub ::get-links
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data :links])))

(reg-sub ::get-links-js
  (fn [[_ viz-id] _]
    (subscribe [::get-links viz-id]))
  (fn [data _]
    (clj->js data)))