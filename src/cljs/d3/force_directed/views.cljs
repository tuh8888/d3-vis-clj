(ns d3.force-directed.views
  (:require [re-frame.core :as rf]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.util :as util :refer [<sub >evt]]
            [d3.force-directed.subs-evts :as ses]))

(defn link-did-mount
  [node viz-id _]
  (let [{:keys [stroke-width stroke]} (<sub [::ses/link-config viz-id])]
    (rid3-> node
      {:stroke-width stroke-width
       :stroke       stroke})))

(defn node-or-text-did-mount
  [node viz-id {:keys [ons]}]
  (-> node
      (.call (<sub [::ses/drag-fn viz-id]))
      (util/set-ons
        (merge {:mouseover #(>evt [::ses/set-hovered viz-id %2 true])
                :mouseout  #(>evt [::ses/set-hovered viz-id %2 false])}
               ons))))

(defn node-did-mount
  [node viz-id {:keys [fill-fn stroke-fn] :as node-opts}]
  (-> node
      (rid3-> {:r      (<sub [::ses/node-size viz-id])
               :fill   fill-fn
               :stroke stroke-fn})
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


(defn force-viz-graph [viz-id {:keys [node-opts link-opts]}]
  [rid3/viz
   {:id     (str (name viz-id) "-graph")
    :ratom  (rf/subscribe [:common.subs/viz viz-id])
    :svg    {:did-mount  #(rf/dispatch-sync [::ses/init-force-viz viz-id])
             :did-update #(rid3-> %
                            {:width  (<sub [::ses/width viz-id])
                             :height (<sub [::ses/height viz-id])
                             :style  {:background-color "grey"}})}

    :pieces [{:kind            :elem-with-data
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
