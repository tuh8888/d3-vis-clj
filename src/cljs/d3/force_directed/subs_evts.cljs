(ns d3.force-directed.subs-evts
  (:require [re-frame.core :as rf :refer [reg-event-db reg-event-fx reg-fx
                                          reg-sub subscribe
                                          trim-v]]
            [d3-vis-clj.db :as db]
            [d3.force-directed.layout :as layout]
            [d3-vis-clj.util :as util]))

(reg-event-fx ::init-force-viz
  [trim-v]
  (fn [{:keys [db]} [viz-id node opts]]
    {:db         (-> db
                     (assoc viz-id db/default-force-layout)
                     (assoc-in [viz-id :svg] node))
     :dispatch-n (list [::initialize-viz-resize viz-id opts]
                       [::initialize-sim viz-id opts])}))

(reg-event-fx ::initialize-viz-resize
  [trim-v]
  (fn [_ [viz-id opts]]
    (let [init-width  js/window.innerWidth
          init-height js/window.innerHeight]
      {:window/on-resize {:dispatch [::viz-resize viz-id opts]}
       :dispatch         [::viz-resize viz-id opts init-width init-height]})))

(reg-event-db ::viz-resize
  [trim-v]
  (fn [db [viz-id {:keys [width height]} new-width new-height]]
    (-> db
        (assoc-in [viz-id :width] (* width new-width))
        (assoc-in [viz-id :height] (* height new-height)))))

(reg-sub ::height
  (fn [db [_ viz-id]]
    (get-in db [viz-id :height])))

(reg-sub ::width
  (fn [db [_ viz-id]]
    (get-in db [viz-id :width])))

(reg-sub ::svg
  (fn [db [_ viz-id]]
    (get-in db [viz-id :svg])))

(reg-event-db ::set-node-elems
  [trim-v (util/path-nth)]
  (fn [db [elems]]
    (assoc-in db [:elems :node] elems)))

(reg-event-db ::set-link-elems
  [trim-v (util/path-nth)]
  (fn [db [elems]]
    (assoc-in db [:elems :link] elems)))

(reg-event-db ::set-link-text-elems
  [trim-v (util/path-nth)]
  (fn [db [elems]]
    (assoc-in db [:elems :link-text] elems)))

(reg-event-db ::set-text-elems
  [trim-v (util/path-nth)]
  (fn [db [elems]]
    (assoc-in db [:elems :text] elems)))

(reg-event-db ::resize-nodes
  [trim-v (util/path-nth)]
  (fn [db [size]]
    (assoc-in db [:node-config :r] size)))

(reg-event-db ::initialize-sim
  [trim-v]
  (fn [db [viz-id opts]]
    (update db viz-id merge
            (layout/new-sim (rf/subscribe [:common.subs/viz viz-id])
                            opts))))

(reg-event-db ::set-node-to-add
  [trim-v (util/path-nth)]
  (fn [db [node-id]]
    (assoc-in db [:node-to-add] (keyword node-id))))

(reg-event-fx ::add-node
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

(reg-event-fx ::expand-node
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

      {:db           (-> db
                         (assoc-in [viz-id :data :nodes] nodes)
                         (assoc-in [viz-id :data :links] links))
       ::restart-sim [config nodes links]})))

(reg-fx ::restart-sim
  (fn [[config nodes links]]
    (layout/restart config :nodes nodes :links links)))

(reg-event-db ::toggle-hovered-node
  [trim-v (util/path-nth) (rf/path [:data :nodes])]
  (fn [db [i]]
    (update-in db [i :hovered] not)))

(reg-event-db ::toggle-hovered-link
  [trim-v (util/path-nth) (rf/path [:data :links])]
  (fn [db [i]]
    (update-in db [i :hovered] not)))

(reg-event-db ::toggle-selected-node
  [trim-v (util/path-nth)]
  (fn [db [i]]
    (update-in db [:data :nodes i :selected] not)))

(reg-sub ::node-size
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-config :r])))

(reg-sub ::node-to-add
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-to-add])))

(reg-sub ::link-config
  (fn [db [_ viz-id]]
    (get-in db [viz-id :link-config])))

(reg-sub ::node
  (fn [db [_ viz-id i]]
    (get-in db [viz-id :data :nodes i])))

(reg-sub ::link
  (fn [db [_ viz-id i]]
    (get-in db [viz-id :data :links i])))

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