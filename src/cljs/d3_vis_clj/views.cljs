(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]
            [reagent.core :as reagent]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Util Fns

(defonce app-state (reagent/atom {:data [{:x 5}
                                         {:x 2}
                                         {:x 3}]}))

(defn get-width [ratom]
  (:width @ratom))

(defn get-height [ratom]
  (let [width (get-width ratom)]
    (* 0.8 width)))

(defn get-value [d]
  (gobj/get d "value"))

(defn get-data [ratom]
  (:data @ratom))

(def cursor-key :bar-simple)

(def margin {:top    8
             :left   32
             :right  16
             :bottom 40})

(defn translate [left top]
  (str "translate("
       (or left 0)
       ","
       (or top 0)
       ")"))

(def color
  (-> js/d3
      (.scaleOrdinal #js ["#3366CC"
                          "#DC3912"
                          "#FF9900"])))

(defn prepare-dataset [ratom]
  (-> @ratom
      (get :dataset)
      clj->js))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Scales

(defn ->x-scale [ratom]
  (let [{:keys [dataset]} @ratom
        labels (mapv :label dataset)]
    (-> js/d3
        .scaleBand
        (.rangeRound #js [0 (get-width ratom)])
        (.padding 0.1)
        (.domain (clj->js labels)))))

(defn ->y-scale [ratom]
  (let [{:keys [dataset]} @ratom
        values    (mapv :value dataset)
        max-value (apply max values)]
    (-> js/d3
        .scaleLinear
        (.rangeRound #js [(get-height ratom) 0])
        (.domain #js [0 max-value]))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page

(defn x-axis []
  {:kind      :container
   :class     "x-axis"
   :did-mount (fn [node ratom]
                (let [x-scale (->x-scale ratom)]
                  (rid3-> node
                          {:transform (translate 0 (get-height ratom))}
                          (.call (.axisBottom js/d3 x-scale)))))})

(defn y-axis []
  {:kind  :container
   :class "y-axis"
   :did-mount
          (fn [node ratom]
            (let [y-scale (->y-scale ratom)]
              (rid3-> node
                      (.call (-> (.axisLeft js/d3 y-scale)
                                 (.ticks 3))))))})

(defn get-label
  [d]
  (gobj/get d "label"))

(defn bar-chart []
  {:kind            :elem-with-data
   :class           "bars"
   :tag             "rect"
   :prepare-dataset prepare-dataset
   :did-mount       (fn [node ratom]
                      (let [y-scale (->y-scale ratom)
                            x-scale (->x-scale ratom)]
                        (rid3-> node
                                {:x      (fn [d] (x-scale (get-label d)))
                                 :width  (.bandwidth x-scale)
                                 :fill   (fn [d i] (color i))
                                 :height (fn [d]
                                           (- (get-height ratom)
                                              (y-scale (get-value d))))
                                 :y      (fn [d] (y-scale (get-value d)))})))})



(defn d3-vis-component []
  [rid3/viz
   {:id             (name cursor-key)
    :ratom          (rf/subscribe [:db])
    :svg            {:did-mount
                     (fn [node ratom]
                       (rid3-> node
                               {:width  (+ (get-width ratom)
                                           (get margin :left)
                                           (get margin :right))
                                :height (+ (get-height ratom)
                                           (get margin :top)
                                           (get margin :bottom))}))}

    :main-container {:did-mount (fn [node _]
                                  (rid3-> node
                                          {:transform (translate
                                                        (get margin :left)
                                                        (get margin :right))}))}
    :pieces         [(x-axis)
                     (y-axis)
                     (bar-chart)]}])

(defn toggle-width-btn
  []
  [:div
   [:button
    {:on-click #(rf/dispatch [:toggle-width])}
    "Toggle width"]])

(defn random-data-btn
  []
  [:div
   [:button
    {:on-click #(rf/dispatch [:generate-random-data])}
    "Randomize data"]])

(defn main-panel []
  #_(let [name (rf/subscribe [:name])]
      (fn []
        [:div "Hello from " @name]))
  [:div
   [:h1 "Bar Chart"]
   [toggle-width-btn]
   [random-data-btn]
   [d3-vis-component]])
