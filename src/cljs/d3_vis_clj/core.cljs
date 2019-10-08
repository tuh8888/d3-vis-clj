(ns d3-vis-clj.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [d3-vis-clj.events]
            [d3-vis-clj.subs]
            [d3-vis-clj.views :as views]
            [d3-vis-clj.config :as config]
            [district0x.re-frame.window-fx]))

(enable-console-print!)

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn render []
  (rf/dispatch-sync [:initialize-db])
  (rf/dispatch-sync [:init-mop-table :panel1])
  (dev-setup)
  (mount-root))
