(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.db :as db]
            [d3.force-directed.layout :as layout]))

(rf/reg-event-db :initialize-db
  (fn [_ _]
    db/default-db))

(rf/reg-event-db :window-resize
  (fn [db _]
    (-> db
        (assoc-in [:height] js/window.innerHeight)
        (assoc-in [:width] js/window.innerWidth))))

(rf/reg-event-db :set-data
  (fn [db [_ viz-name k v]]
    (assoc-in db [viz-name :data k] v)))

(rf/reg-event-db :resize-nodes
  (fn [db [_ viz-name size]]
    (assoc-in db [viz-name :node-config :r] size)))

(rf/reg-event-db :initialize-sim
  (fn [db [_ viz-name]]
    (update db viz-name merge
            (layout/new-sim (rf/subscribe [:force-layout viz-name])))))

(rf/reg-event-db :set-node-to-add
  (fn [db [_ viz-name node-id]]
    (assoc-in db [viz-name :node-to-add] (keyword node-id))))

(rf/reg-event-fx :add-node
  (fn [{:keys [db]} [_ viz-name]]
    (when-let [new-node (get-in db [:all-data :mops @(rf/subscribe [:node-to-add viz-name])])]
      (let [{{{:keys [nodes]} :data :as config} viz-name} db
            nodes (conj nodes new-node)]
        {:restart-sim [config nodes]
         :db          (assoc-in db [viz-name :data :nodes] nodes)}))))

(defn slots->links
  [id slots]
  (for [[role fillers] slots
        filler fillers]
    {:source   id
     :target   filler
     :label    role
     :strength 0.1}))


(rf/reg-event-fx :expand-node
  (fn [{:keys [db]} [_ viz-name i]]
    (let [{{{:keys [links nodes]} :data
            :as                   config} viz-name} db
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
                        (assoc-in [viz-name :data :nodes] nodes)
                        (assoc-in [viz-name :data :links] links))
       :restart-sim [config nodes links]})))

(rf/reg-fx :restart-sim
  (fn [[config nodes links]]
    (layout/restart config :nodes nodes :links links)))

(rf/reg-event-db :set-hovered
  (fn [db [_ viz-name i val]]
    (assoc-in db [viz-name :data :nodes i :hovered] val)))


