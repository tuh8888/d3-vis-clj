(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.db :as db]))

(defn ->mock-dataset []
  (mapv #(hash-map :label %
                   :value (rand-int 200))
        ["A" "B" "C" "D"]))

(rf/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(rf/reg-event-db :toggle-width
                 (fn [db event]
                   (update db :width (fn [width]
                                       (if (= 300 width) 500 300)))))

(def num-nodes 5)
(def num-links 2)
(def max-weight 3)

(defn ->mock-nodes []
  (mapv #(hash-map :id (str "node" %)
                   :label %
                   :group (rand-int 3)
                   :level (rand-int 3))
        (range num-nodes)))

(defn ->mock-links []
  (mapv #(hash-map :source (rand-int num-nodes)
                   :target (rand-int num-nodes)
                   :strength (rand-int max-weight))
        (range num-links)))

(rf/reg-event-db :generate-random-data
  (fn [db event]
    (-> db
        (assoc :dataset (->mock-dataset))
        (assoc-in [:network :nodes] (->mock-nodes))
        (assoc-in [:network :links] (->mock-links)))))

(rf/reg-event-fx
  :window-width
  (fn [{:keys [db]} [_ width]]
    {:db (-> db
             (assoc-in [:test-data :width] width))}))

(rf/reg-event-fx
  :window-height
  (fn [{:keys [db]} [_ height]]
    {:db (-> db
             (assoc-in [:test-data :height] height))}))

(rf/reg-event-db :set-var
  (fn [db [_ k v]]
    (assoc-in db [:test-data :dataset k] v)))

(rf/reg-event-db
  :set-node-var
  (fn [db [_ i k v]]
    (assoc-in db [:test-data :dataset :nodes i k] v)))


