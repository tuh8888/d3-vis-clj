(ns d3-vis-clj.views
  (:require [cljsjs.d3]
            [d3-vis-clj.util :refer [<sub >evt] :as util]
            [common.subs :as c-subs]
            [d3.force-directed.views :as force-views]
            [d3.force-directed.subs-evts :as fses]
            [data-table.views :as dt-views]
            [clojure.string :as str]))

(defn node-size-text-box
  [viz-id]
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     (<sub [::fses/node-size viz-id])
            :on-change #(>evt [::fses/resize-nodes viz-id
                               (util/target-value %)])}]])

(defn node-labels-check-box
  [viz-id]
  [:div
   [:input
    {:type      "checkbox"
     :checked   (<sub [:node-labels? viz-id])
     :on-change #(>evt [:toggle-node-labels viz-id])}]
   [:label "Display node labels"]])

(defn link-labels-check-box
  [viz-id]
  [:div
   [:input
    {:type      "checkbox"
     :checked   (<sub [:link-labels? viz-id])
     :on-change #(>evt [:toggle-link-labels viz-id])}]
   [:label "Display link labels"]])

(defn add-node-btn
  [viz-id]
  [:div
   [:button {:type     "button"
             :on-click #(>evt [::fses/add-node viz-id])}
    "Add Node"]
   [:input {:type      "text"
            :value     (<sub [::fses/node-to-add viz-id])
            :on-change #(>evt [::fses/set-node-to-add viz-id
                               (util/target-value %)])}]])
(defn force-viz
  [viz-id]
  [:div
   {:style {:display "flex"}}
   [:div
    {:style {:width "10%"}}
    [node-size-text-box viz-id]
    [add-node-btn viz-id]
    [node-labels-check-box viz-id]
    [link-labels-check-box viz-id]]
   [:div
    {:style {:flex-grow "1"}}
    [force-views/force-viz-graph viz-id
     {:svg-opts  {:width  0.9
                  :height 0.5}
      :node-opts {:ons       {:click #(if js/d3.event.ctrlKey
                                        (>evt [::fses/expand-node viz-id %2])
                                        (>evt [::fses/toggle-selected-node viz-id %2]))}
                  :fill-fn   #(<sub [:node-color viz-id %2])
                  :stroke-fn #(<sub [:node-stroke viz-id %2])
                  :label-fn  #(<sub [:node-label viz-id %2])}
      :link-opts {:label-fn #(<sub [:link-label viz-id %2])}}]]])

(defn role-aggregation-row
  [viz-id]
  [:tr
   [:th {:col-span 2} ""]
   [:th {:col-span (count (<sub [:visible-roles viz-id]))} "Roles"]])

(defn slot-header-render-fn
  [viz-id role i]
  (fn [_]
    [:div
     [:select
      {:on-change #(>evt [:set-visible-role viz-id
                          (keyword (util/target-value %)) i])
       :value     role}
      (for [-role (<sub [:all-roles viz-id])]
        ^{:key (str (random-uuid))}
        [:option {:value -role} -role])]
     [:button {:type     "button"
               :on-click #(>evt [:toggle-sort-role viz-id role])}
      "sort"]]))

(defn slot-cols
  [viz-id]
  (for [[i role] (map-indexed vector (<sub [:visible-roles viz-id]))]
    {:col-key              [:slots role]
     :col-header-render-fn (slot-header-render-fn viz-id role i)
     :col-header-options   {:class (str/join " "
                                             ["sorted-by"
                                              (if (<sub [:sorted-role? viz-id role])
                                                "asc"
                                                "desc")])}
     :render-fn            (fn [fillers]
                             (str/join ", " fillers))}))

(defn all-roles-check-box
  [viz-id]
  [:div
   [:input
    {:type      "checkbox"
     :checked   (<sub [:all-roles-visible? viz-id])
     :on-change #(>evt [:toggle-all-roles viz-id])}]
   [:label "all"]])

(defn role-selection
  [viz-id]
  (let [all-roles (<sub [:all-roles viz-id])]
    [:div
     "Select Roles "
     [all-roles-check-box viz-id]
     (doall
       (for [role all-roles]
         ^{:key (str (random-uuid))}
         [:div
          [:input
           {:type      "checkbox"
            :checked   (<sub [:visible-role? viz-id role])
            :on-change #(>evt [:toggle-visible-role viz-id role])}]
          [:label role
           [:button
            {:on-click #(>evt [:add-visible-role viz-id role])}
            "+"]]]))]))

(defn mop-table
  "Table for displaying mop data"
  [viz-id]
  [:div
   {:style {:display "flex"}}
   [:div
    {:style {:width    "10%"
             :height   "50vh"
             :overflow "scroll"}}
    [role-selection viz-id]]
   [:div
    {:style {:flex-grow "1"
             :height    "50vh"
             :overflow  "scroll"}}
    [dt-views/data-table
     [:visible-mops viz-id]
     (into [{:col-key [:id]}
            {:col-key [:name]}]
           (slot-cols viz-id))
     {:header      (role-aggregation-row viz-id)
      :row-options (fn [id]
                     {:on-click #(>evt [:toggle-selected-mop viz-id id])
                      :style    {:fill "blue"}
                      :class    [(when (<sub [::c-subs/selected? viz-id id])
                                   "selected")]})}]]])

(defn main-panel []
  [:div
   [:h1 (<sub [:name])]
   [:div
    [mop-table :panel1]
    [force-viz :force-viz1]]])