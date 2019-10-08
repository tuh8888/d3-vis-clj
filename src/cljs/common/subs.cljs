(ns common.subs
  (:require [re-frame.core :refer [subscribe reg-sub]]))

(reg-sub ::viz
  (fn [db [_ viz-id]]
    (get db viz-id)))

(reg-sub ::selected
  (fn [db [_ viz-id]]
    (get-in db [viz-id :selected])))

(reg-sub ::selected?
  (fn [[_ viz-id] _]
    (subscribe [::selected viz-id]))
  (fn [selected [_ _ id]]
    (contains? selected id)))
