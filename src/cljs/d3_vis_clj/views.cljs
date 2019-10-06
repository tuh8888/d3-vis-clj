(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [goog.object :as gobj]
            [d3-vis-clj.util :as util]))

(def <sub (comp deref re-frame.core/subscribe))             ;; aka listen (above)
(def >evt re-frame.core/dispatch)

(defn link-did-mount
  [node viz-name]
  (let [{:keys [stroke-width stroke]} @(rf/subscribe [:link-config viz-name])]
    (rid3-> node
            {:stroke-width stroke-width
             :stroke       stroke})))

(defn node-color
  [{:keys [id hovered]}]
  (cond hovered "yellow"
        (isa? @(rf/subscribe [:hierarchy]) id :A) "red"
        (isa? @(rf/subscribe [:hierarchy]) id :B) "blue"
        :default "green"))

(defn node-did-mount
  [node viz-name ratom]
  (-> node
      (rid3-> {:r    (get-in @ratom [:node-config :r])
               :fill (fn [_ i] (node-color (get-in @ratom [:data :nodes i])))})
      (.call (or (:drag @ratom) #()))
      (util/set-ons
        :mouseover (fn [_ i] (>evt [:set-hovered viz-name i true]))
        :mouseout (fn [_ i] (>evt [:set-hovered viz-name i false]))
        :click (fn [_ i] (>evt [:expand-node viz-name i])))))

(defn text-did-mount
  [node _]
  (-> node
      (rid3-> {:text-anchor "middle"})
      (.text (fn [d] (gobj/get d "name")))))

(defn prepare-data
  [viz-name var]
  (-> (<sub [:force-layout viz-name])
      (get-in [:data var])
      (clj->js)))

(defn force-viz [viz-name ratom]
  [rid3/viz
   {:id     (name viz-name)
    :ratom  ratom
    :svg    {:did-mount  (fn [_ v]
                           (rf/dispatch-sync [:initialize-force-layout viz-name]))


             :did-update (fn [node ratom]
                           (let [{:keys [width height]} @ratom]
                             (rid3-> node
                                     {:width  width
                                      :height height
                                      :style  {:background-color "grey"}})))}

    :pieces [{:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node _]
                                 (rf/dispatch-sync
                                   [:set-data viz-name
                                    :link-elems (link-did-mount node viz-name)]))
              :prepare-dataset (fn [_] (prepare-data viz-name :links))}
             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node ratom]
                                 (rf/dispatch-sync
                                   [:set-data viz-name
                                    :node-elems (node-did-mount node viz-name ratom)]))
              :prepare-dataset (fn [_] (prepare-data viz-name :nodes))}
             {:kind            :elem-with-data
              :tag             "text"
              :class           "texts"
              :did-mount       (fn [node _]
                                 (rf/dispatch-sync
                                   [:set-data viz-name
                                    :text-elems (text-did-mount node viz-name)]))
              :prepare-dataset (fn [_] (prepare-data viz-name :nodes))}]}])


(defn node-size-text-box []
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     @(rf/subscribe [:node-size :network])
            :on-change #(rf/dispatch [:resize-nodes :network
                                      (util/text-value %)])}]])

(defn add-node-btn
  []
  [:div
   [:button {:type     "button"
             :on-click #(rf/dispatch [:add-node :network])}
    "Add Node"]
   [:input {:type      "text"
            :value     @(rf/subscribe [:node-to-add :network])
            :on-change #(rf/dispatch [:set-node-to-add :network
                                      (util/text-value %)])}]])

(defn main-panel []
  [:div
   [:h1 @(rf/subscribe [:name])]
   [node-size-text-box]
   [add-node-btn]
   (let [viz-name :network
         ratom    (rf/subscribe [:force-layout viz-name])]
     [force-viz viz-name ratom])])