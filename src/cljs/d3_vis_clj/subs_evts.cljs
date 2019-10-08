(ns d3-vis-clj.subs-evts
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf :refer [subscribe reg-sub
                                          reg-event-db reg-event-fx
                                          trim-v]]
            [d3.force-directed.subs-evts :as fses]
            [d3-vis-clj.db :as db]
            [clojure.set :as set]
            [d3-vis-clj.util :as util]))

(reg-event-db :initialize-db
  (fn [_ _]
    db/default-db))

(reg-sub :initial-data
  (fn [_ [_ _]]
    db/default-force-data))


(reg-event-db :window-resized
  [trim-v]
  (fn [db [new-width new-height]]
    (-> db
        (assoc-in [:height] new-height)
        (assoc-in [:width] new-width))))

(reg-event-fx :initialize-window-resize
  [trim-v]
  (fn [_ _]
    (let [init-width  js/window.innerWidth
          init-height js/window.innerHeight]
      {:window/on-resize {:dispatch [:window-resize]}
       :dispatch         [:window-resize init-width init-height]})))

(reg-sub :window-height
  (fn [db]
    (:height db)))

(reg-sub :window-width
  (fn [db]
    (:width db)))

(reg-event-db :toggle-selected-mop
  [trim-v (util/path-nth)]
  (fn [db [id]]
    (update-in db [:selected] #(util/toggle-contains-set % id))))

(reg-event-db :toggle-sort-role
  [trim-v (util/path-nth)]
  (fn [db [role]]
    (let [sorted? (get-in db [:sorted-roles role])]
      (-> db
          (update-in [:sorted-roles] #(if sorted?
                                        (disj % role)
                                        (conj (or % #{}) role)))
          (update-in [:data]
                     (fn [data]
                       (let [data (sort-by #(let [v (get-in % [:slots role])]
                                              (if (coll? v)
                                                (first v)
                                                v))
                                           data)]
                         (if sorted?
                           data
                           (reverse data)))))))))

(reg-sub :sorted-roles
  (fn [db [_ viz-id]]
    (get-in db [viz-id :sorted-roles])))

(reg-sub :sorted-role?
  (fn [[_ viz-id] _]
    (subscribe [:sorted-roles viz-id]))
  (fn [roles [_ _ role]]
    (contains? roles role)))

(reg-event-db :init-mop-table
  [trim-v]
  (fn [db [viz-id]]
    (assoc-in db [viz-id :data] (vals (get-in db [:all-data :mops])))))

(reg-event-db :toggle-visible-role
  [trim-v (util/path-nth)]
  (fn [db [role]]
    (update-in db [:visible-roles]
               #(util/toggle-contains-vector (or % []) role))))

(reg-event-db :set-visible-role
  [trim-v (util/path-nth)]
  (fn [db [role i]]
    (assoc-in db [:visible-roles i] role)))

(reg-sub :visible-role?
  (fn [[_ viz-id] _]
    (subscribe [:visible-roles viz-id]))
  (fn [roles [_ _ role]]
    (some #(= role %) roles)))

(reg-event-db :add-visible-role
  [trim-v (util/path-nth)]
  (fn [db [role]]
    (update-in db [:visible-roles] #(conj (or % []) role))))

(reg-sub :visible-roles
  (fn [db [_ viz-id]]
    (get-in db [viz-id :visible-roles])))

(rf/reg-cofx :all-roles
  (fn [{[viz-id] :event :as coeffects}]
    (assoc coeffects :all-roles @(rf/subscribe [:all-roles viz-id])
                     :all-roles-visible? @(rf/subscribe [:all-roles-visible? viz-id]))))

(reg-event-fx :toggle-all-roles
  [trim-v (rf/inject-cofx :all-roles)]
  (fn [{:keys [all-roles db all-roles-visible?]}
       [viz-id]]
    {:db (update-in db [viz-id :visible-roles]
                    (fn [roles]
                      (if all-roles-visible?
                        []
                        (->> roles
                             (set)
                             (set/difference all-roles)
                             (into roles)))))}))

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

(reg-event-db :toggle-node-labels
  [trim-v (util/path-nth)
   (rf/path [:node-config :show-labels])]
  not)

(reg-sub :node-labels?
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-config :show-labels])))

(reg-sub :node-label
  (fn [[_ viz-id i] _]
    [(subscribe [:node-labels? viz-id])
     (subscribe [::fses/node viz-id i])])
  (fn [[show-labels node] _]
    (when (or (:selected node)
              (:hovered node)
              show-labels)
      (:name node))))

(reg-event-db :toggle-link-labels
  (fn [db [_ viz-id]]
    (update-in db [viz-id :link-config :show-labels] not)))

(reg-sub :link-labels?
  (fn [db [_ viz-id]]
    (get-in db [viz-id :link-config :show-labels])))

(reg-sub :link-label
  (fn [[_ viz-id i] _]
    [(subscribe [:link-labels? viz-id])
     (subscribe [::fses/link viz-id i])])
  (fn [[show-labels link] _]
    (when (or (:hovered link)
              show-labels)
      (:label link))))

(reg-sub :name
  (fn [db]
    (:name db)))

(reg-sub :hierarchy
  (fn [db]
    (get-in db [:all-data :hierarchy])))

(reg-sub :visible-mops
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data])))

(reg-sub :node-color
  (fn [[_ viz-id i] _]
    [(subscribe [:hierarchy]) (subscribe [::fses/node viz-id i])])
  (fn [[h {:keys [id hovered]}] _]
    (cond hovered "yellow"
          (isa? h id :A) "cyan"
          (isa? h id :B) "light-grey"
          :default "white")))

(reg-sub :node-stroke
  (fn [[_ viz-id i]]
    (subscribe [::fses/node viz-id i]))
  (fn [node _]
    (if (:selected node)
      "red"
      "white")))

(reg-sub :link-stroke
  (fn [[_ viz-id i]]
    (subscribe [::fses/link viz-id i]))
  (fn [link _]
    (if (:selected link)
      "red"
      "#E5E5E5")))