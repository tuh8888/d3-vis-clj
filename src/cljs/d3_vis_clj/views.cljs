(ns d3-vis-clj.views
  (:require [cljsjs.d3]
            [d3-vis-clj.util :refer [<sub >evt] :as util]
            [d3.force-directed.views :as force-views]
            [d3.force-directed.subs :as force-subs]
            [d3.force-directed.events :as force-evts]
            [clojure.string :as str]))

(defn node-size-text-box
  [viz-id]
  [:div
   "Node size: "
   [:input {:type      "text"
            :value     (<sub [::force-subs/node-size viz-id])
            :on-change #(>evt [::force-evts/resize-nodes viz-id
                               (util/text-value %)])}]])

(defn add-node-btn
  [viz-id]
  [:div
   [:button {:type     "button"
             :on-click #(>evt [::force-evts/add-node viz-id])}
    "Add Node"]
   [:input {:type      "text"
            :value     (<sub [::force-subs/node-to-add viz-id])
            :on-change #(>evt [::force-evts/set-node-to-add viz-id
                               (util/text-value %)])}]])
(defn force-viz
  [viz-id]
  [:div
   [node-size-text-box viz-id]
   [add-node-btn viz-id]
   [force-views/force-viz-graph viz-id
    {:node-opts {:ons       {:click #(if js/d3.event.ctrlKey
                                       (>evt [::force-evts/expand-node viz-id %2])
                                       (>evt [::force-evts/toggle-selected-node viz-id %2]))}
                 :fill-fn   #(<sub [::force-subs/node-color viz-id %2])
                 :stroke-fn #(<sub [::force-subs/node-outer-color viz-id %2])}}]])

(defn data-table
  [data-sub col-defs {:keys [header row-options]}]
  [:table.ui.table
   [:thead
    header
    [:tr
     (doall
       (for [{:keys [col-key col-header-render-fn
                     col-header-options]
              :or   {col-header-render-fn last}} col-defs]
         ^{:key (str (random-uuid))}
         [:th
          col-header-options
          (col-header-render-fn col-key)]))]]
   [:tbody
    (doall
      (for [{:keys [id] :as item} (<sub data-sub)]
        ^{:key (str (random-uuid))}
        [:tr
         (row-options id)
         (for [{:keys [col-key render-fn]} col-defs
               :let [val (get-in item col-key)]]
           ^{:key (str (random-uuid))}
           [:td
            (if render-fn (render-fn val) val)])]))]])

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
                          (keyword (-> % .-target .-value)) i])
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
   [role-selection viz-id]
   [data-table
    [:visible-mops viz-id]
    (into [{:col-key [:id]}
           {:col-key [:name]}]
          (slot-cols viz-id))
    {:header      (role-aggregation-row viz-id)
     :row-options (fn [id]
                    {:on-click #(>evt [:toggle-selected-mop viz-id id])
                     :style    {:fill "blue"}
                     :class    [(when (<sub [:selected-mop? viz-id id])
                                  "selected")]})}]])

(defn main-panel []
  [:div
   [:h1 (<sub [:name])]
   [mop-table :panel1]
   [force-viz :force-viz1]])