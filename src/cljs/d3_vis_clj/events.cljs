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

(rf/reg-event-db :add-node
  (fn [db [_ viz-name]]
    (if-let [new-node (get-in db [:all-data :mops @(rf/subscribe [:node-to-add viz-name])])]
      (let [{{{:keys [nodes]} :data :as config} viz-name} db
            nodes (conj nodes new-node)]
        (layout/restart config :nodes nodes)
        (assoc-in db [viz-name :data :nodes] nodes))
      db)))

(rf/reg-event-db :expand-node
  (fn [db [_ viz-name i]]
    (let [{{{:keys [nodes links]} :data
            :as config} viz-name} db
          {:keys [id slots]} (get-in db [viz-name :data :nodes i])
          node-ids (->> nodes
                        (map :id)
                        (set))
          link-set (->> links
                        (map #(select-keys % [:source
                                              :target
                                              :label]))
                        (set))
          new-links  (->> (for [[e ns] slots
                                n ns]
                            (hash-map :source id
                                      :target n
                                      :label e
                                      :strength 0.1))
                          (remove link-set))
          new-nodes (->> new-links
                         (map :target)
                         (map #(get-in db [:all-data :mops %]))
                         (remove node-ids))

          nodes (into nodes new-nodes)
          links (into links new-links)]
      (layout/restart config :nodes nodes :links links)
      (-> db
          (assoc-in [viz-name :data :nodes] nodes)
          (assoc-in [viz-name :data :links] links)))))

(rf/reg-event-db :set-hovered
  (fn [db [_ viz-name i val]]
    (assoc-in db [viz-name :data :nodes i :hovered ] val)))


