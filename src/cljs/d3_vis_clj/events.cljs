(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.db :as db]
            [d3.force-directed.layout :as layout]))

(rf/reg-event-db :initialize-db
  (fn [_ _]
    db/default-db))

(rf/reg-event-db :window-resized
  (fn [db [_ viz-name new-width new-height]]
    (-> db
        (assoc-in [:height] new-height)
        (assoc-in [:width] new-width)
        (assoc-in [viz-name :height] new-height)
        (assoc-in [viz-name :width] new-width))))

(rf/reg-event-fx :initialize-window-resize
  (fn [{:keys [db]} [_ viz-name init-width init-height]]
    {:window/on-resize {:dispatch [:window-resized viz-name]}
     :db               (-> db
                           (assoc-in [:height] init-height)
                           (assoc-in [:width] init-width)
                           (assoc-in [viz-name :height] init-height)
                           (assoc-in [viz-name :width] init-width))}))

(rf/reg-event-fx :init-force-viz
  (fn [{:keys [db]} [_ viz-name]]
    {:db         (assoc db viz-name db/default-force-layout)
     :dispatch-n (list [:initialize-window-resize viz-name
                        js/window.innerWidth js/window.innerHeight]
                       [:initialize-sim viz-name])}))

(rf/reg-event-db :set-node-elems
  (fn [db [_ viz-name elems]]
    (assoc-in db [viz-name :elems :node] elems)))

(rf/reg-event-db :set-link-elems
  (fn [db [_ viz-name elems]]
    (assoc-in db [viz-name :elems :link] elems)))

(rf/reg-event-db :set-text-elems
  (fn [db [_ viz-name elems]]
    (assoc-in db [viz-name :elems :text] elems)))

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
    (let [node-id  (get-in db [viz-name :node-to-add])
          new-node (get-in db [:all-data :mops node-id])]
      (when new-node
        (let [{{{:keys [nodes]} :data :as config} viz-name} db
              nodes (conj nodes new-node)]
          {:restart-sim [config nodes]
           :db          (assoc-in db [viz-name :data :nodes] nodes)})))))

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

(rf/reg-event-db :set-selected-mop
  (fn [db [_ viz-name {:keys [id]}]]
    (assoc-in db [viz-name :selected] id)))


