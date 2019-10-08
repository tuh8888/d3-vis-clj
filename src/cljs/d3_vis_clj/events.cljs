(ns d3-vis-clj.events
  (:require [re-frame.core :as rf :refer [reg-event-db reg-event-fx trim-v]]
            [d3-vis-clj.db :as db]
            [clojure.set :as set]
            [d3-vis-clj.util :as util]))

(reg-event-db :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db :window-resized
  [trim-v]
  (fn [db [viz-id new-width new-height]]
    (-> db
        (assoc-in [:height] new-height)
        (assoc-in [:width] new-width)
        (assoc-in [viz-id :height] new-height)
        (assoc-in [viz-id :width] new-width))))

(reg-event-fx :initialize-window-resize
  [trim-v]
  (fn [{:keys [db]} [viz-id init-width init-height]]
    {:window/on-resize {:dispatch [:window-resized viz-id]}
     :db               (-> db
                           (assoc-in [:height] init-height)
                           (assoc-in [:width] init-width)
                           (assoc-in [viz-id :height] init-height)
                           (assoc-in [viz-id :width] init-width))}))

(reg-event-db :toggle-selected-mop
  [util/viz-id-interceptor trim-v]
  (fn [db [id]]
    (update-in db [:selected] #(util/toggle-contains-set % id))))

(reg-event-db :toggle-sort-role
  [util/viz-id-interceptor trim-v]
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

(reg-event-db :init-mop-table
  [trim-v]
  (fn [db [viz-id]]
    (assoc-in db [viz-id :data] (vals (get-in db [:all-data :mops])))))

(reg-event-db :toggle-visible-role
  [util/viz-id-interceptor trim-v]
  (fn [db [role]]
    (update-in db [:visible-roles]
               #(util/toggle-contains-vector (or % []) role))))

(reg-event-db :set-visible-role
  [util/viz-id-interceptor trim-v]
  (fn [db [role i]]
    (assoc-in db [:visible-roles i] role)))

(reg-event-db :add-visible-role
  [util/viz-id-interceptor trim-v]
  (fn [db [role]]
    (update-in db [:visible-roles] #(conj (or % []) role))))

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

