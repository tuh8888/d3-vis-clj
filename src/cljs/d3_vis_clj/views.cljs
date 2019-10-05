(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.d3-force :as force]
            [d3-vis-clj.util :as util]))

(defn get-node-color [d]
  (let [id (keyword (util/get-id d))]
    (cond (isa? @(rf/subscribe [:hierarchy]) id :A) "red"
          (isa? @(rf/subscribe [:hierarchy]) id :B) "blue"
          :default "green")))

(defn force-viz [ratom]
  [rid3/viz
   {:id     "force"
    :ratom  ratom
    :svg    {:did-mount (fn [node _]
                          (let [[width height] @(rf/subscribe [:window-dims])]
                            (rid3-> node
                                    {:width  width
                                     :height height
                                     :style  {:background-color "grey"}})))}

    :pieces [{:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       (fn [node _]
                                 (let [{:keys [stroke-width stroke]} @(rf/subscribe [:link-config :network])
                                       link-elems (rid3-> node
                                                          {:stroke-width stroke-width
                                                           :stroke       stroke})]
                                   (rf/dispatch-sync [:set-data :network :link-elems link-elems])))
              :prepare-dataset (fn [_]
                                 (clj->js @(rf/subscribe [:get-data :network :links])))}

             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       (fn [node _]
                                 (let [node-elems (-> node
                                                      (rid3-> {:r    @(rf/subscribe [:node-size :network])
                                                               :fill get-node-color})
                                                      (force/call-drag @(rf/subscribe [:sim :network])))]
                                   (rf/dispatch-sync [:set-data :network :node-elems node-elems])))
              :prepare-dataset (fn [_]
                                 (clj->js @(rf/subscribe [:get-data :network :nodes])))}]}])

(defn node-size-text-box []
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     @(rf/subscribe [:node-size :network])
            :on-change #(rf/dispatch [:resize-nodes :network  (util/text-value %)])}]])

(defn add-node-btn
  []
  [:div
   [:button {:type     "button"
             :on-click #(rf/dispatch [:add-node :network])}
    "Add Node"]
   [:input {:type "text"
            :value @(rf/subscribe [:node-to-add :network])
            :on-change #(rf/dispatch [:set-node-to-add :network (util/text-value %)])}]])

(defn main-panel []
  [:div
   [:h1 @(rf/subscribe [:name])]
   [node-size-text-box]
   [add-node-btn]
   (let [data (rf/subscribe [:viz :network])]
     [force-viz data])])