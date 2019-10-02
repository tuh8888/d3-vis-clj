(ns d3-vis-clj.events
  (:require [re-frame.core :as rf]
            [d3-vis-clj.db :as db]))

(defn ->mock-dataset []
  (mapv #(hash-map :label %
                   :value (rand-int 200))
        ["A" "B" "C"]))

(rf/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(rf/reg-event-db :toggle-width
                 (fn [db event]
                   (update db :width (fn [width]
                                       (if (= 300 width) 500 300)))))

(rf/reg-event-db :generate-random-data
                 (fn [db event]
                   (assoc db :dataset (->mock-dataset))))
