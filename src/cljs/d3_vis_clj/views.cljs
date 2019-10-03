(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]
            [d3-vis-clj.util :as util]))

(def link-force
  (-> js/d3
      .forceLink
      (.id util/get-id)
      (.strength (fn [link]
                   (gobj/get link "strength")))))

(def charge-force
  (-> (js/d3.forceManyBody)
      (.strength -120)))

(defn center-force [ratom]
  (-> js/d3
      (.forceCenter
        (/ (:width @ratom) 2)
        (/ (:height @ratom) 2))))

(defn collide-force [ratom]
  (-> js/d3
      (.forceCollide)
      (.radius (:r ratom))))

(defn sim-did-update [ratom]
  (let [sim          (-> js/d3
                         .forceSimulation
                         (.force "link" link-force)
                         (.force "collide" (collide-force ratom))
                         (.force "charge" charge-force)
                         (.force "center" (center-force ratom)))
        node-dataset (clj->js (-> @ratom
                                  :dataset
                                  :nodes))
        link-dataset (clj->js (-> @ratom
                                  :dataset
                                  :links))
        node-elems   @(rf/subscribe [:get-var :node-elems])
        link-elems   @(rf/subscribe [:get-var :link-elems])

        tick-handler (fn []
                       (rid3-> node-elems
                               {:cx (fn [_ idx]
                                      (-> node-dataset
                                          (get idx)
                                          (.-x)))
                                :cy (fn [_ idx]
                                      (-> node-dataset
                                          (get idx)
                                          (.-y)))})
                       (rid3-> link-elems
                               {:x1 (fn [_ idx]
                                      (-> link-dataset
                                          (get idx)
                                          (.-source)
                                          (.-x)))
                                :y1 (fn [_ idx]
                                      (-> link-dataset
                                          (get idx)
                                          (.-source)
                                          (.-y)))
                                :x2 (fn [_ idx]
                                      (-> link-dataset
                                          (get idx)
                                          (.-target)
                                          (.-x)))
                                :y2 (fn [_ idx]
                                      (-> link-dataset
                                          (get idx)
                                          (.-target)
                                          (.-y)))}))]


    ;; Add notes to simulation
    (-> sim
        (.nodes node-dataset)
        (.on "tick" tick-handler))

    ;; Add link force to simulation
    (-> sim
        (.force "link")
        (.links link-dataset))
    (rf/dispatch-sync [:set-var :sim sim])))

(defn drag-started [d idx]
  (println "drag started" d)
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (-> sim
                (.nodes)
                (get idx))]
    (println "drag started" d)
    (when-not (-> js/d3
                  (.-event)
                  (.-active))
      (println "restart")
      (-> sim
          (.alphaTarget 0.3)
          (.restart)))
    (set! (.-fx d) (.-x d))
    (set! (.-fy d) (.-y d))))


(defn dragged [d idx]
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (-> sim
                (.nodes)
                (get idx))]
    (->> js/d3.event
         (.-x)
         (set! (.-fx d)))
    (set! (.-fy d) (.-y js/d3.event))))

(defn drag-ended [d idx]
  (println "drag ended")
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (-> sim
                (.nodes)
                (get idx))]
    (when-not (-> js/d3
                  (.-event)
                  (.-active))
      (-> sim
          (.alphaTarget 0)))
    (set! (.-fx d) nil)
    (set! (.-fy d) nil)))


(defn force-viz [ratom]
  [rid3/viz
   {:id     "force"
    :ratom  ratom
    :svg    {:did-mount (fn [node ratom]
                          (rid3-> node
                                  {:width  (:width @ratom)
                                   :height (:height @ratom)
                                   :style  {:background-color "grey"}}))}

    :pieces [{:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node ratom]
                                 (let [r (-> node
                                             (rid3-> {:r    (:r @ratom)
                                                      :fill (:node-color @ratom)})
                                             (.call (-> js/d3
                                                        (.drag)
                                                        (.on "start" drag-started)
                                                        (.on "drag" dragged)
                                                        (.on "end" drag-ended))))]
                                   (rf/dispatch-sync [:set-var :node-elems r])))
              :prepare-dataset (fn [ratom]
                                 (->> @ratom
                                      :dataset
                                      :nodes
                                      (clj->js)))}

             {:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node ratom]
                                 (let [r (rid3-> node
                                                 {:stroke-width 1
                                                  :stroke       "#E5E5E5"})]
                                   (rf/dispatch-sync [:set-var :link-elems r])))
              :prepare-dataset (fn [ratom]
                                 (->> @ratom
                                      :dataset
                                      :links
                                      (clj->js)))}
             {:kind       :raw
              :did-mount  sim-did-update
              :did-update sim-did-update}]}])



(defn main-panel []
  (rf/dispatch-sync [:window-width js/window.innerWidth])
  (rf/dispatch-sync [:window-height js/window.innerHeight])

  (let [data (rf/subscribe [:data])]
    [force-viz data]))