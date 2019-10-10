(ns d3-vis-clj.db
  (:require [data-table.db :as dt-db]))

(def default-force-data
  {:data {:nodes []
          :links []}})

(def default-table-data
  (merge
    dt-db/default-data
    {:data []}))

(def default-db
  (assoc {:name "d3-vis-clj"}
    :panel1 default-table-data
    :force-viz1 default-force-data))