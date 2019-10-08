(ns d3-vis-clj.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]))

(rf/reg-sub :name
  (fn [db]
    (:name db)))

(rf/reg-sub :window-height
  (fn [db]
    (:height db)))

(rf/reg-sub :window-width
  (fn [db]
    (:width db)))

(rf/reg-sub :hierarchy
  (fn [db]
    (get-in db [:all-data :hierarchy])))

(rf/reg-sub :force-layout
  (fn [db [_ viz-id]]
    (get db viz-id)))

(rf/reg-sub :node-size
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-config :r])))

(rf/reg-sub :node-to-add
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-to-add])))

(rf/reg-sub :link-config
  (fn [db [_ viz-id]]
    (get-in db [viz-id :link-config])))

(rf/reg-sub :get-node
  (fn [db [_ viz-id i]]
    (get-in db [viz-id :data :nodes i])))

(rf/reg-sub :node-color
  (fn [[_ viz-id i] _]
    [(rf/subscribe [:hierarchy]) (rf/subscribe [:get-node viz-id i])])
  (fn [[h {:keys [id hovered]}] _]
    (cond hovered "yellow"
          (isa? h id :A) "red"
          (isa? h id :B) "blue"
          :default "green")))

(rf/reg-sub :node-name
  (fn [[_ viz-id i] _]
    (rf/subscribe [:get-node viz-id i]))
  (fn [{:keys [name]} _]
    name))

(rf/reg-sub :drag-fn
  (fn [db [_ viz-id]]
    (get-in db [viz-id :drag] #())))

(rf/reg-sub :get-nodes
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data :nodes])))

(rf/reg-sub :get-nodes-js
  (fn [[_ viz-id] _]
    (rf/subscribe [:get-nodes viz-id]))
  (fn [data _]
    (clj->js data)))

(rf/reg-sub :get-links
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data :links])))

(rf/reg-sub :get-links-js
  (fn [[_ viz-id] _]
    (rf/subscribe [:get-links viz-id]))
  (fn [data _]
    (clj->js data)))

(rf/reg-sub :visible-mops
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data])))

(rf/reg-sub :selected-mops
  (fn [db [_ viz-id]]
    (get-in db [viz-id :selected])))

(rf/reg-sub :mop-id
  (fn [_ [_ mop]]
    (:id mop)))

(rf/reg-sub :visible-roles
  (fn [db [_ viz-id]]
    (get-in db [viz-id :visible-roles])))

(rf/reg-sub :reversed-col
  (fn [db [_ viz-id]]
    (get-in db [viz-id :reversed-col])))

(rf/reg-sub :rev?
  (fn [[_ viz-id] _]
    (rf/subscribe [:reversed-col viz-id]))
  (fn [rev [_ _ col-key]]
    (= col-key rev)))

(rf/reg-sub :selected-mop?
  (fn [[_ viz-id] _]
    (rf/subscribe [:selected-mops viz-id]))
  (fn [selected-mops [_ _ id]]
    (contains? selected-mops id)))

(rf/reg-sub :visible-role?
  (fn [[_ viz-id] _]
    (rf/subscribe [:visible-roles viz-id]))
  (fn [roles [_ _ role]]
    (some #(= role %) roles)))

(rf/reg-sub :all-roles
  (fn [[_ viz-id] _]
    (rf/subscribe [:visible-mops viz-id]))
  (fn [mops _]
    (->> mops
         (map :slots)
         (mapcat keys)
         (set))))

(rf/reg-sub :all-roles-visible?
  (fn [[_ viz-id] _]
    [(rf/subscribe [:all-roles viz-id]) (rf/subscribe [:visible-roles viz-id])])
  (fn [[all-roles visible-roles] _]
    (every? (set visible-roles) all-roles)))