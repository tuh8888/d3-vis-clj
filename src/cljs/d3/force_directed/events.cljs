(ns d3.force-directed.events
  (:require [re-frame.core :as rf :refer [reg-event-db reg-event-fx reg-fx
                                          trim-v]]
            [d3-vis-clj.db :as db]
            [d3.force-directed.layout :as layout]
            [d3-vis-clj.util :as util]))

(reg-event-fx ::init-force-viz
  [trim-v]
  (fn [{:keys [db]} [viz-id]]
    {:db         (assoc db viz-id db/default-force-layout)
     :dispatch-n (list [:initialize-window-resize viz-id
                        js/window.innerWidth js/window.innerHeight]
                       [::initialize-sim viz-id])}))

(reg-event-db ::set-node-elems
  [util/viz-id-interceptor trim-v]
  (fn [db [elems]]
    (assoc-in db [:elems :node] elems)))

(reg-event-db ::set-link-elems
  [util/viz-id-interceptor trim-v]
  (fn [db [elems]]
    (assoc-in db [:elems :link] elems)))

(reg-event-db ::set-link-text-elems
  [util/viz-id-interceptor trim-v]
  (fn [db [elems]]
    (assoc-in db [:elems :link-text] elems)))

(reg-event-db ::set-text-elems
  [util/viz-id-interceptor trim-v]
  (fn [db [elems]]
    (assoc-in db [:elems :text] elems)))

(reg-event-db ::resize-nodes
  [util/viz-id-interceptor trim-v]
  (fn [db [size]]
    (assoc-in db [:node-config :r] size)))

(reg-event-db ::initialize-sim
  [trim-v]
  (fn [db [viz-id]]
    (update db viz-id merge
            (layout/new-sim (rf/subscribe [:common.subs/viz viz-id])))))

(reg-event-db ::set-node-to-add
  [util/viz-id-interceptor trim-v]
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

(reg-event-db ::set-hovered
  [util/viz-id-interceptor trim-v]
  (fn [db [i val]]
    (assoc-in db [:data :nodes i :hovered] val)))

(reg-event-db ::toggle-selected-node
  [util/viz-id-interceptor trim-v]
  (fn [db [i]]
    (let [node (get-in db [:data :nodes i])]
      (update-in db [:selected :node] #(if (= (:id %) (:id node))
                                         nil
                                         node)))))
