(ns d3.force-directed.views
  (:require [re-frame.core :as rf]
            [rid3.core :as rid3 :refer [rid3->]]
            [d3-vis-clj.util :as util :refer [<sub >evt]]
            [d3.force-directed.subs :as subs]
            [d3.force-directed.events :as evts]))

(defn link-did-mount
  [node viz-id]
  (let [{:keys [stroke-width stroke]} (<sub [::subs/link-config viz-id])]
    (rid3-> node
      {:stroke-width stroke-width
       :stroke       stroke})))

(defn node-or-text-did-mount
  [node viz-id {:keys [ons]}]
  (-> node
      (.call (<sub [::subs/drag-fn viz-id]))
      (util/set-ons
        (merge {:mouseover #(>evt [::evts/set-hovered viz-id %2 true])
                :mouseout  #(>evt [::evts/set-hovered viz-id %2 false])}
               ons))))

(defn node-did-mount
  [node viz-id {:keys [fill-fn stroke-fn] :as node-opts}]
  (-> node
      (rid3-> {:r      (<sub [::subs/node-size viz-id])
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
  [node viz-id]
  (-> node
      (rid3-> {:text-anchor "middle"})
      (.text #(<sub [::subs/link-name viz-id %2]))))


(defn force-viz-graph [viz-id {:keys [node-opts]}]
  [rid3/viz
   {:id     (str (name viz-id) "-graph")
    :ratom  (rf/subscribe [:common.subs/viz viz-id])
    :svg    {:did-mount  #(rf/dispatch-sync [::evts/init-force-viz viz-id])
             :did-update #(rid3-> %
                            {:width  (<sub [:window-width])
                             :height (<sub [:window-height])
                             :style  {:background-color "grey"}})}

    :pieces [{:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       #(rf/dispatch-sync
                                  [::evts/set-link-elems viz-id
                                   (link-did-mount % viz-id)])
              :prepare-dataset #(<sub [::subs/get-links-js viz-id])}
             {:kind            :elem-with-data
              :tag             "text"
              :class           "link-text"
              :did-mount       #(rf/dispatch-sync
                                  [::evts/set-link-text-elems viz-id
                                   (link-text-did-mount % viz-id)])
              :prepare-dataset #(<sub [::subs/get-links-js viz-id])}
             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       #(rf/dispatch-sync
                                  [::evts/set-node-elems viz-id
                                   (node-did-mount % viz-id node-opts)])
              :prepare-dataset #(<sub [::subs/get-nodes-js viz-id])}
             {:kind            :elem-with-data
              :tag             "text"
              :class           "texts"
              :did-mount       #(rf/dispatch-sync
                                  [::evts/set-text-elems viz-id
                                   (text-did-mount % viz-id node-opts)])
              :prepare-dataset #(<sub [::subs/get-nodes-js viz-id])}]}])
