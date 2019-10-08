(ns d3-vis-clj.events
  (:require [re-frame.core :as rf :refer [reg-event-db reg-event-fx reg-fx
                                          ->interceptor
                                          get-coeffect assoc-coeffect
                                          get-effect assoc-effect
                                          trim-v]]
            [d3-vis-clj.db :as db]
            [d3.force-directed.layout :as layout]))

(defn remove-nth
  [v n]
  (-> v
      (subvec 0 n)
      (concat (subvec v (inc n)))
      (vec)))

(def viz-id-interceptor
  (let [db-store-key     :re-frame-path/db-store
        viz-id-store-key :viz-id-store]
    (->interceptor
      :id :viz-id-path
      :before (fn [context]
                (let [original-db (get-coeffect context :db)
                      viz-id      (get-in context [:coeffects :event 1])
                      new-db      (get original-db viz-id)]
                  (-> context
                      (update-in [:coeffects :event] remove-nth 1)
                      (update db-store-key conj original-db)
                      (assoc viz-id-store-key viz-id)
                      (assoc-coeffect :db new-db))))

      :after (fn [context]
               (let [db-store     (get context db-store-key)
                     original-db  (peek db-store)
                     new-db-store (pop db-store)
                     viz-id       (get context viz-id-store-key)
                     context'     (-> context
                                      (assoc db-store-key new-db-store)
                                      (assoc-coeffect :db original-db)) ;; put the original db back so that things like debug work later on
                     db           (get-effect context :db ::not-found)]
                 (if (= db ::not-found)
                   context'
                   (->> db
                        (assoc original-db viz-id)
                        (assoc-effect context' :db))))))))

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

(reg-event-fx :init-force-viz
  [trim-v]
  (fn [{:keys [db]} [viz-id]]
    {:db         (assoc db viz-id db/default-force-layout)
     :dispatch-n (list [:initialize-window-resize viz-id
                        js/window.innerWidth js/window.innerHeight]
                       [:initialize-sim viz-id])}))

(reg-event-db :set-node-elems
  [viz-id-interceptor trim-v]
  (fn [db [elems]]
    (assoc-in db [:elems :node] elems)))

(reg-event-db :set-link-elems
  [viz-id-interceptor trim-v]
  (fn [db [elems]]
    (assoc-in db [:elems :link] elems)))

(reg-event-db :set-text-elems
  [viz-id-interceptor trim-v]
  (fn [db [elems]]
    (assoc-in db [:elems :text] elems)))

(reg-event-db :resize-nodes
  [viz-id-interceptor trim-v]
  (fn [db [size]]
    (assoc-in db [:node-config :r] size)))

(reg-event-db :initialize-sim
  [trim-v]
  (fn [db [viz-id]]
    (update db viz-id merge
            (layout/new-sim (rf/subscribe [:force-layout viz-id])))))

(reg-event-db :set-node-to-add
  [viz-id-interceptor trim-v]
  (fn [db [node-id]]
    (assoc-in db [:node-to-add] (keyword node-id))))

(reg-event-fx :add-node
  [trim-v]
  (fn [{:keys [db]} [viz-id]]
    (let [node-id  (get-in db [viz-id :node-to-add])
          new-node (get-in db [:all-data :mops node-id])]
      (when new-node
        (let [{{{:keys [nodes]} :data :as config} viz-id} db
              nodes (conj nodes new-node)]
          {:restart-sim [config nodes]
           :db          (assoc-in db [viz-id :data :nodes] nodes)})))))

(defn slots->links
  [id slots]
  (for [[role fillers] slots
        filler fillers]
    {:source   id
     :target   filler
     :label    role
     :strength 0.1}))

(reg-event-fx :expand-node
  [trim-v]
  (fn [{:keys [db]} [viz-id i]]
    (let [{{{:keys [links nodes]} :data
            :as                   config} viz-id} db
          {{:keys [id slots]} i} nodes
          link-keys    [:source :target :label]
          old-links    (->> links
                            (map #(select-keys % link-keys))
                            (set))
          old-link?    (fn [link] (old-links (select-keys link link-keys)))
          links        (->> slots
                            (slots->links id)
                            (remove old-link?)
                            (into links))
          old-node-ids (->> nodes
                            (map :id)
                            (set))
          nodes        (->> slots
                            (mapcat second)
                            (map #(get-in db [:all-data :mops %]))
                            (remove #(old-node-ids (:id %)))
                            (into nodes))]

      {:db          (-> db
                        (assoc-in [viz-id :data :nodes] nodes)
                        (assoc-in [viz-id :data :links] links))
       :restart-sim [config nodes links]})))

(reg-fx :restart-sim
  (fn [[config nodes links]]
    (layout/restart config :nodes nodes :links links)))

(reg-event-db :set-hovered
  [viz-id-interceptor trim-v]
  (fn [db [i val]]
    (assoc-in db [:data :nodes i :hovered] val)))

(defn toggle-contains
  [coll x]
  (if (contains? coll x)
    (disj coll x)
    (conj (or coll #{}) x)))

(reg-event-db :toggle-selected-mop
  [viz-id-interceptor trim-v]
  (fn [db [id]]
    (update-in db [:selected] #(toggle-contains % id))))

(reg-event-db :set-sort-key
  [viz-id-interceptor trim-v]
  (fn [db [col-key rev?]]
    (-> db
        (assoc-in [:reversed-col] (when-not rev? col-key))
        (update-in [:data]
                   (fn [data]
                     (let [data (sort-by #(let [v (get-in % col-key)]
                                            (if (coll? v)
                                              (first v)
                                              v))
                                         data)]
                       (if rev?
                         data
                         (reverse data))))))))

(reg-event-db :init-mop-table
  [trim-v]
  (fn [db [viz-id]]
    (assoc-in db [viz-id :data] (vals (get-in db [:all-data :mops])))))

(defn toggle-contains-vector
  [coll x]
  (if (some #(= x %) coll)
    (->> coll
         (remove #(= x %))
         (vec))
    (conj (or coll []) x)))

(reg-event-db :toggle-visible-role
  [viz-id-interceptor trim-v]
  (fn [db [role]]
    (update-in db [:visible-roles] #(toggle-contains-vector (or % []) role))))

(reg-event-db :set-visible-role
  [viz-id-interceptor trim-v]
  (fn [db [role i]]
    (assoc-in db [:visible-roles i] role)))

(reg-event-db :add-visible-role
  [viz-id-interceptor trim-v]
  (fn [db [role]]
    (update-in db [:visible-roles] #(conj (or % []) role))))

(reg-event-db :toggle-all-roles
  [viz-id-interceptor trim-v]
  (fn [db []]
    db))