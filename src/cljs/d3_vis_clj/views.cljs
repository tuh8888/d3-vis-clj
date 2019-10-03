(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]
            [d3-vis-clj.util :as util]))

(defn sim-did-update [ratom]
  (let [sim          (-> (js/d3.forceSimulation)
                         (.force "link" (.id (-> js/d3 .forceLink) (fn [d] (.-id d))))
                         (.force "charge" (js/d3.forceManyBody))
                         (.force "center" (js/d3.forceCenter (/ (:width @ratom) 2)
                                                             (/ (:height @ratom) 2))))
        node-dataset (clj->js (-> @ratom
                                  (get :dataset)
                                  (get :nodes)))
        link-dataset (clj->js (-> @ratom
                                  (get :dataset)
                                  (get :links)))
        node-elems   @(rf/subscribe [:get-var :node-elems])
        link-elems   @(rf/subscribe [:get-var :link-elems])

        tick-handler (fn []
                       (-> node-elems
                           (.attr "cx" (fn [_ idx]
                                         (.-x (get node-dataset idx))))
                           (.attr "cy" (fn [_ idx]
                                         (.-y (get node-dataset idx)))))

                       (-> link-elems
                           (.attr "x1" (fn [_ idx]
                                         (-> (get link-dataset idx)
                                             .-source .-x)))
                           (.attr "y1" (fn [_ idx]
                                         (-> (get link-dataset idx)
                                             .-source .-y)))
                           (.attr "x2" (fn [_ idx]
                                         (-> (get link-dataset idx)
                                             .-target .-x)))
                           (.attr "y2" (fn [_ idx]
                                         (-> (get link-dataset idx)
                                             .-target .-y)))))]


    ;; Add notes to simulation
    (-> sim
        (.nodes node-dataset)
        (.on "tick" tick-handler))

    ;; Add link force to simulation
    (-> sim
        (.force "link")
        (.links link-dataset))))

(defn drag-started [d idx]
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (-> sim .nodes (get idx))]
    (when (= 0 (-> js/d3 .-event .-active))
      (-> sim (.alphaTarget 0.3) (.restart)))
    (set! (.-fx d) (.-x d))
    (set! (.-fy d) (.-y d))))


(defn dragged [_ idx]
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (-> sim .nodes (get idx))]
    (set! (.-fx d) (.-x js/d3.event))
    (set! (.-fy d) (.-y js/d3.event))))

(defn drag-ended [_ idx]
  (let [sim @(rf/subscribe [:get-var :sim])
        d   (-> sim .nodes (get idx))]
    (when (= 0 (-> js/d3 .-event .-active))
      (-> sim (.alphaTarget 0)))
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
                                 (let [r (rid3-> node
                                                 {:r    5
                                                  :fill "red"})]
                                   (rf/dispatch-sync [:set-nodes r])))
              :prepare-dataset (fn [ratom]
                                (let [nodes (->> @ratom
                                                 :dataset
                                                 :nodes)]
                                  (clj->js nodes)))}

             {:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node ratom]
                                 (let [r (rid3-> node
                                                 {:stroke-width 1
                                                  :stroke       "#E5E5E5"})]
                                   (rf/dispatch-sync [:set-links r])))
              :prepare-dataset (fn [ratom]
                                 (->> @ratom
                                      :dataset
                                      :links
                                      clj->js))}
             {:kind       :raw
              :did-mount  sim-did-update
              :did-update sim-did-update}]}])



(defn main-panel []
  (rf/dispatch-sync [:window-width js/window.innerWidth])
  (rf/dispatch-sync [:window-height js/window.innerHeight])

  (let [data (rf/subscribe [:data])]
    [force-viz data]))