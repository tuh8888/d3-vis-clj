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

(reg-sub :force-layout
  (fn [db [_ viz-id]]
    (get db viz-id)))

(reg-sub :node-size
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-config :r])))

(reg-sub :node-to-add
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-to-add])))

(reg-sub :link-config
  (fn [db [_ viz-id]]
    (get-in db [viz-id :link-config])))

(reg-sub :get-node
  (fn [db [_ viz-id i]]
    (get-in db [viz-id :data :nodes i])))

(reg-sub :node-color
  (fn [[_ viz-id i] _]
    [(subscribe [:hierarchy]) (subscribe [:get-node viz-id i])])
  (fn [[h {:keys [id hovered]}] _]
    (cond hovered "yellow"
          (isa? h id :A) "red"
          (isa? h id :B) "blue"
          :default "green")))

(reg-sub :node-name
  (fn [[_ viz-id i] _]
    (subscribe [:get-node viz-id i]))
  (fn [{:keys [name]} _]
    name))

(reg-sub :drag-fn
  (fn [db [_ viz-id]]
    (get-in db [viz-id :drag] #())))

(reg-sub :get-nodes
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data :nodes])))

(reg-sub :get-nodes-js
  (fn [[_ viz-id] _]
    (subscribe [:get-nodes viz-id]))
  (fn [data _]
    (clj->js data)))

(reg-sub :get-links
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data :links])))

(reg-sub :get-links-js
  (fn [[_ viz-id] _]
    (subscribe [:get-links viz-id]))
  (fn [data _]
    (clj->js data)))

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

(reg-sub :reversed-col
  (fn [db [_ viz-id]]
    (get-in db [viz-id :reversed-col])))

(reg-sub :rev?
  (fn [[_ viz-id] _]
    (subscribe [:reversed-col viz-id]))
  (fn [rev [_ _ col-key]]
    (= col-key rev)))

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