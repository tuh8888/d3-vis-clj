(ns d3-vis-clj.views
  (:require [re-frame.core :as rf]
            [cljsjs.d3]
            [rid3.core :as rid3 :refer [rid3->]]
            [re-frame-datatable.core :as dt]
            [d3-vis-clj.util :refer [<sub >evt] :as util]))

(defn link-did-mount
  [node viz-id]
  (let [{:keys [stroke-width stroke]} (<sub [:link-config viz-id])]
    (rid3-> node
      {:stroke-width stroke-width
       :stroke       stroke})))

(defn node-or-text-did-mount
  [node viz-id]
  (-> node
      (.call (<sub [:drag-fn viz-id]))
      (util/set-ons
        :mouseover #(>evt [:set-hovered viz-id %2 true])
        :mouseout #(>evt [:set-hovered viz-id %2 false])
        :click #(>evt [:expand-node viz-id %2]))))

(defn node-did-mount
  [node viz-id]
  (-> node
      (rid3-> {:r    (<sub [:node-size viz-id])
               :fill #(<sub [:node-color viz-id %2])})
      (node-or-text-did-mount viz-id)))

(defn text-did-mount
  [node viz-id]
  (-> node
      (rid3-> {:text-anchor "middle"})
      (.text #(<sub [:node-name viz-id %2]))
      (node-or-text-did-mount viz-id)))

(defn force-viz-graph [viz-id]
  [rid3/viz
   {:id     (str (name viz-id) "-graph")
    :ratom  (rf/subscribe [:force-layout viz-id])
    :svg    {:did-mount  #(rf/dispatch-sync [:init-force-viz viz-id])

             :did-update #(rid3-> %
                            {:width  (<sub [:window-width])
                             :height (<sub [:window-height])
                             :style  {:background-color "grey"}})}

    :pieces [{:kind            :elem-with-data
              :tag             "line"
              :class           "link"
              :did-mount       #(rf/dispatch-sync
                                  [:set-link-elems viz-id
                                   (link-did-mount % viz-id)])
              :prepare-dataset #(<sub [:get-links-js viz-id])}
             {:kind            :elem-with-data
              :tag             "circle"
              :class           "node"
              :did-mount       #(rf/dispatch-sync
                                  [:set-node-elems viz-id
                                   (node-did-mount % viz-id)])
              :prepare-dataset #(<sub [:get-nodes-js viz-id])}
             {:kind            :elem-with-data
              :tag             "text"
              :class           "texts"
              :did-mount       #(rf/dispatch-sync
                                  [:set-text-elems viz-id
                                   (text-did-mount % viz-id)])
              :prepare-dataset #(<sub [:get-nodes-js viz-id])}]}])

(defn node-size-text-box
  [viz-id]
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     (<sub [:node-size viz-id])
            :on-change #(>evt [:resize-nodes viz-id
                               (util/text-value %)])}]])

(defn add-node-btn
  [viz-id]
  [:div
   [:button {:type     "button"
             :on-click #(>evt [:add-node viz-id])}
    "Add Node"]
   [:input {:type      "text"
            :value     (<sub [:node-to-add viz-id])
            :on-change #(>evt [:set-node-to-add viz-id
                               (util/text-value %)])}]])

(defn force-viz
  [viz-id]
  [:div
   [node-size-text-box viz-id]
   [add-node-btn viz-id]
   [force-viz-graph viz-id]])

(defn table-cell
  [viz-name mop role]
  [:td
   [:button {:type     "button"
             :on-click #(>evt [:set-selected-mop viz-name mop])
             :style    {:background-color (<sub [:panel-item-color viz-name mop])}}
    (:name mop)]])

(defn table-row
  [viz-name mop]
  [:tr
   (for [role (<sub [:visible-roles viz-name])]
     [table-cell viz-name mop role])])

(defn table-header
  [viz-name]
  [:tr
   (for [role (<sub [:visible-roles viz-name])]
     [:th role])])

(defn mop-table
  [viz-name]
  [:div
   "Selected: " (<sub [:selected-mop viz-name])
   [:table
    [table-header viz-name]
    (for [mop (<sub [:visible-mops viz-name])]
      [table-row viz-name mop])]])

(defn main-panel []
  [:div
   [:h1 (<sub [:name])]
   [mop-table :panel1]
   #_[force-viz :force-viz1]])