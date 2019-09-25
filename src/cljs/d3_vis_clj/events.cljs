(ns d3-vis-clj.events
  (:require [re-frame.core :as re-frame]
            [d3-vis-clj.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))
