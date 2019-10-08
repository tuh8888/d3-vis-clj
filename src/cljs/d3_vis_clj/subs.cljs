(ns d3-vis-clj.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe reg-sub]]))

(reg-sub :name
  (fn [db]
    (:name db)))

(reg-sub :window-height
  (fn [db]
    (:height db)))

(reg-sub :window-width
  (fn [db]
    (:width db)))

(reg-sub :hierarchy
  (fn [db]
    (get-in db [:all-data :hierarchy])))

(reg-sub :visible-mops
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data])))

(reg-sub :selected-mops
  (fn [db [_ viz-id]]
    (get-in db [viz-id :selected])))

(reg-sub :mop-id
  (fn [_ [_ mop]]
    (:id mop)))

(reg-sub :visible-roles
  (fn [db [_ viz-id]]
    (get-in db [viz-id :visible-roles])))

(reg-sub :sorted-roles
  (fn [db [_ viz-id]]
    (get-in db [viz-id :sorted-roles])))

(reg-sub :sorted-role?
  (fn [[_ viz-id] _]
    (subscribe [:sorted-roles viz-id]))
  (fn [roles [_ _ role]]
    (println roles)
    (contains? roles role)))

(reg-sub :selected-mop?
  (fn [[_ viz-id] _]
    (subscribe [:selected-mops viz-id]))
  (fn [selected-mops [_ _ id]]
    (contains? selected-mops id)))

(reg-sub :visible-role?
  (fn [[_ viz-id] _]
    (subscribe [:visible-roles viz-id]))
  (fn [roles [_ _ role]]
    (some #(= role %) roles)))

(reg-sub :all-roles
  (fn [[_ viz-id] _]
    (subscribe [:visible-mops viz-id]))
  (fn [mops _]
    (->> mops
         (map :slots)
         (mapcat keys)
         (set))))

(reg-sub :all-roles-visible?
  (fn [[_ viz-id] _]
    [(subscribe [:all-roles viz-id]) (subscribe [:visible-roles viz-id])])
  (fn [[all-roles visible-roles] _]
    (every? (set visible-roles) all-roles)))