(ns d3.force-directed.zoom
  (:require [cljsjs.d3]
            [d3-vis-clj.util :as d3-util]
            [rid3.core :refer [rid3->]]))

(defn zoom
  [g]
  (-> (js/d3.zoom)
      (.scaleExtent (clj->js [0.1 7]))
      (d3-util/set-ons
        {:zoom (fn []
                 (let [transform js/d3.event.transform]
                   (rid3-> g
                     {:transform transform})))})))