(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]
            [d3-vis-clj.util :as util]))

(def link-force
  (-> js/d3
      (.forceLink)
      (.id util/get-id)
      (.strength  (fn [link]
                    (gobj/get link "strength")))
      (.distance 1)))

(def charge-force
  (-> js/d3
      (.forceManyBody)
      (.strength -120)))

(defn center-force [ratom]
  (-> js/d3
      (.forceCenter (/ (:width @ratom) 2)
                    (/ (:height @ratom) 2))))

(defn collide-force [ratom]
  (-> js/d3
      (.forceCollide)
      (.radius (:r ratom))))

(defn prepare-data [ratom v]
  (-> @ratom
      :dataset
      (get v)
      (clj->js)))

(defn translate [x y]
  (str "translate(" x "," y ")"))

(defn tick-handler [sim node-elems link-elems]
  (fn []
    (rid3-> node-elems
            {:transform (fn [d idx]
                          (translate (-> sim
                                         .nodes
                                         (get idx)
                                         (.-x))
                                     (-> sim
                                         .nodes
                                         (get idx)
                                         (.-y))))})

    (rid3-> link-elems
            {:x1 (fn [_ idx]
                   (-> sim
                       (.force "link")
                       (.links)
                       (get idx)
                       (.-source)
                       (.-x)))
             :y1 (fn [_ idx]
                   (-> sim
                       (.force "link")
                       (.links)
                       (get idx)
                       (.-source)
                       (.-y)))
             :x2 (fn [_ idx]
                   (-> sim
                       (.force "link")
                       (.links)
                       (get idx)
                       (.-target)
                       (.-x)))
             :y2 (fn [_ idx]
                   (-> sim
                       (.force "link")
                       (.links)
                       (get idx)
                       (.-target)
                       (.-y)))})))

(defn sim-did-mount [ratom]
  (let [sim          (-> js/d3
                         (.forceSimulation)
                         (.force "link" link-force)
                         (.force "collide" (collide-force ratom))
                         (.force "charge" charge-force)
                         (.force "center" (center-force ratom)))
        node-dataset (prepare-data ratom :nodes)
        link-dataset (prepare-data ratom :links)
        node-elems   @(rf/subscribe [:get-var :node-elems])
        link-elems   @(rf/subscribe [:get-var :link-elems])]
    (-> sim
        (.nodes node-dataset)
        (.on "tick" (tick-handler sim node-elems link-elems)))
    (-> sim
        (.force "link")
        (.links link-dataset))
    (rf/dispatch [:set-var :sim sim])))

(defn sim-did-update [ratom])

(defn drag-started [d idx]
  (println "start")
  (let [sim @(rf/subscribe [:get-var :sim])
        d (-> sim .nodes (get idx))]
    (when (= 0 (-> js/d3 .-event .-active))
      (-> sim (.alphaTarget 0.3) (.restart)))
    (set! (.-fx d) (.-x d))
    (set! (.-fy d) (.-y d))))

(defn dragged [_ idx]
  (let [sim @(rf/subscribe [:get-var :sim])
        d (-> sim .nodes (get idx))]
    (set! (.-fx d) (.-x js/d3.event))
    (set! (.-fy d) (.-y js/d3.event))))

(defn drag-ended [_ idx]
  (println "end")
  (let [sim @(rf/subscribe [:get-var :sim])
        d (-> sim .nodes (get idx))]
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
                                 (let [config (:node-config @ratom)
                                       r (-> node
                                             (rid3-> {:r    (:r config)
                                                      :fill (:fill config)})
                                             (.call (-> js/d3
                                                        (.drag)
                                                        (.on "start" drag-started)
                                                        (.on "drag" dragged)
                                                        (.on "end" drag-ended))))]
                                   (rf/dispatch-sync [:set-var :node-elems r])))
              :prepare-dataset #(prepare-data % :nodes)}

             {:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node ratom]
                                 (let [r (rid3-> node
                                                 {:stroke-width 1
                                                  :stroke       "#E5E5E5"})]
                                   (rf/dispatch-sync [:set-var :link-elems r])))
              :prepare-dataset #(prepare-data % :links)}
             {:kind       :raw
              :did-mount  sim-did-mount
              :did-update sim-did-update}]}])



(defn main-panel []
  (rf/dispatch-sync [:window-width js/window.innerWidth])
  (rf/dispatch-sync [:window-height js/window.innerHeight])

  (let [data (rf/subscribe [:data])]
    [force-viz data]))