(ns d3.force-directed.views
  (:require [re-frame.core :as rf]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.util :as util :refer [<sub >evt]]
            [d3.force-directed.subs-evts :as ses]
            [d3.force-directed.interaction :as force-interaction]))

(defn link-did-mount
  [node viz-id {:keys [ons style]}]
  (-> node
      (rid3->
        {:style style})
      (util/set-ons
        (merge {:mouseover #(>evt [::ses/toggle-hovered-link viz-id %2])
                :mouseout  #(>evt [::ses/toggle-hovered-link viz-id %2])}
               ons))))

(defn node-or-text-did-mount
  [node viz-id {:keys [ons]}]
  (-> node
      (.call (<sub [::ses/drag-fn viz-id]))
      (util/set-ons
        (merge {:mouseover #(>evt [::ses/toggle-hovered-node viz-id %2])
                :mouseout  #(>evt [::ses/toggle-hovered-node viz-id %2])}
               ons))))

(defn node-did-mount
  [node viz-id {:keys [style] :as node-opts}]
  (-> node
      (rid3-> {:r     (<sub [::ses/node-size viz-id])
               :style style})
      (node-or-text-did-mount viz-id node-opts)))

(defn text-did-mount
  [node viz-id {:as   node-opts
                :keys [label-fn]}]
  (-> node
      (rid3-> {:text-anchor "middle"})
      (.text label-fn)
      (node-or-text-did-mount viz-id node-opts)))

(defn link-text-did-mount
  [node _ {:keys [label-fn]}]
  (-> node
      (rid3-> {:text-anchor "middle"})
      (.text label-fn)))

(defn force-viz-graph [viz-id {:keys                                    [svg-opts node-opts link-opts]
                               {:keys [zoom-fn]
                                :or   {zoom-fn force-interaction/zoom}} :svg-opts}]
  [rid3/viz
   {:id             (str (name viz-id) "-graph")
    :ratom          (rf/subscribe [:common.subs/viz viz-id])
    :svg            {:did-mount  (fn [node _]
                                   (rf/dispatch-sync [::ses/init-force-viz viz-id
                                                      node svg-opts]))

                     :did-update (fn [node _]
                                   (rid3-> node
                                     {:width  (<sub [::ses/width viz-id])
                                      :height (<sub [::ses/height viz-id])
                                      :style  {:background-color "grey"}}))}

    :main-container {:did-mount  (fn [node _]
                                   (.call (<sub [::ses/svg viz-id])
                                          (zoom-fn node)))
                     :did-update #()}

    :pieces         [{:kind            :elem-with-data
                      :tag             "line"
                      :class           "link"
                      :did-mount       #(rf/dispatch-sync
                                          [::ses/set-link-elems viz-id
                                           (link-did-mount % viz-id link-opts)])
                      :prepare-dataset #(<sub [::ses/get-links-js viz-id])}
                     {:kind            :elem-with-data
                      :tag             "text"
                      :class           "link-text"
                      :did-mount       #(rf/dispatch-sync
                                          [::ses/set-link-text-elems viz-id
                                           (link-text-did-mount % viz-id link-opts)])
                      :prepare-dataset #(<sub [::ses/get-links-js viz-id])}
                     {:kind            :elem-with-data
                      :tag             "circle"
                      :class           "node"
                      :did-mount       #(rf/dispatch-sync
                                          [::ses/set-node-elems viz-id
                                           (node-did-mount % viz-id node-opts)])
                      :prepare-dataset #(<sub [::ses/get-nodes-js viz-id])}
                     {:kind            :elem-with-data
                      :tag             "text"
                      :class           "texts"
                      :did-mount       #(rf/dispatch-sync
                                          [::ses/set-text-elems viz-id
                                           (text-did-mount % viz-id node-opts)])
                      :prepare-dataset #(<sub [::ses/get-nodes-js viz-id])}]}])
