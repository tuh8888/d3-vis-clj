(ns d3-vis-clj.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe reg-sub]]
            [d3.force-directed.subs :as force-subs]))

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
    (contains? roles role)))

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

(reg-sub :node-color
  (fn [[_ viz-id i] _]
    [(subscribe [:hierarchy]) (subscribe [::force-subs/get-node viz-id i])])
  (fn [[h {:keys [id hovered]}] _]
    (cond hovered "yellow"
          (isa? h id :A) "red"
          (isa? h id :B) "blue"
          :default "green")))


(reg-sub :node-labels?
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-config :show-labels])))

(reg-sub :node-label
  (fn [[_ viz-id i] _]
    [(subscribe [:node-labels? viz-id])
     (subscribe [::force-subs/get-node viz-id i])])
  (fn [[show-labels node] _]
    (when show-labels
      (:name node))))
