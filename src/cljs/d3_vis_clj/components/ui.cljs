(ns d3-vis-clj.components.ui
  (:require [com.stuartsierra.component :as component]
            [d3-vis-clj.core :refer [render]]))

(defrecord UIComponent []
  component/Lifecycle
  (start [component]
    (render)
    component)
  (stop [component]
    component))

(defn new-ui-component []
  (map->UIComponent {}))
