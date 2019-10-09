(ns data-table.subs-evts
  (:require [re-frame.core :refer [subscribe reg-sub
                                   reg-event-db reg-event-fx
                                   trim-v path]]))


(reg-sub ::row-value
  (fn [db [_ viz-id i]]
    (get-in db [viz-id :data i])))