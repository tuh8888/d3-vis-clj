(ns d3-vis-clj.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [d3-vis-clj.events]
            [d3-vis-clj.subs]
            [d3-vis-clj.views :as views]
            [d3-vis-clj.config :as config]))

(enable-console-print!)

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn render []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
