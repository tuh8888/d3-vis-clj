(ns d3-vis-clj.views
  (:require [cljsjs.d3]
            [d3-vis-clj.util :refer [<sub >evt] :as util]
            [d3.force-directed.views :as force-views]
            [d3.force-directed.subs-evts :as fses]
            [data-table.views :as dt-views]
            [d3.force-directed.interaction :as force-interaction]
            [clojure.string :as str]))

(defn node-size-text-box
  [viz-id]
  [:div
   "Node size: "
   [:input {:type      "range"
            :min       "1"
            :max       "100"
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
  (let [node-list-id (str (random-uuid))
        nodes        (<sub [:all-mops])
        do-action    #(>evt [::fses/add-node viz-id
                             (<sub [:mop nil (<sub [:node-to-add viz-id])])])]
    [:div
     [:input {:type         "text"
              :list         node-list-id
              :value        (<sub [:node-to-add viz-id])
              :on-change    #(>evt [:set-node-to-add viz-id
                                    (util/target-value %)])
              :on-key-press #(when (= (.-charCode %) 13)
                               (do-action))}]
     [:datalist {:id node-list-id}
      (for [mop nodes]
        ^{:key (random-uuid)}
        [:option {:value (:id mop)}
         (:name mop)])]
     [:button {:type     "button"
               :on-click do-action}
      "Add Node"]]))


(defn flex-div
  [width height left right]
  [:div {:style {:display "flex"}}
   [:div {:style {:width    width
                  :height   height
                  :overflow "scroll"}}
    left]
   [:div {:style {:flex-grow "1"
                  :height    height
                  :overflow  "scroll"}}
    right]])
(defn force-viz
  [viz-id width]
  [flex-div width "40%"
   [:div
    [node-labels-check-box viz-id]
    [link-labels-check-box viz-id]
    [node-size-text-box viz-id]
    [add-node-btn viz-id]]
   [:div
    [force-views/force-viz-graph viz-id
     {:svg-opts  {:width   (- 1 (-> width
                                    (str/replace #"\%" "")
                                    (int)
                                    (/ 100)))
                  :height  0.4
                  :zoom-fn force-interaction/zoom
                  :drag-fn force-interaction/drag}
      :node-opts {:ons      {:click #(if js/d3.event.ctrlKey
                                       (>evt [::fses/expand-node viz-id %2])
                                       (>evt [::fses/toggle-selected-node viz-id %2]))}
                  :style    {:fill   #(<sub [:mop-color viz-id %2])
                             :stroke #(<sub [:node-stroke viz-id %2])}
                  :label-fn #(<sub [:node-label viz-id %2])}
      :link-opts {:label-fn #(<sub [:link-label viz-id %2])
                  :style    {:stroke-width 4
                             :stroke       #(<sub [:link-stroke viz-id %2])}
                  :ons      {:click #(>evt [::fses/toggle-selected-link viz-id %2])}}}]]])
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
      {:on-change #(>evt [:set-visible-role viz-id i
                          (keyword (util/target-value %))])
       :value     role}
      (for [-role (<sub [:all-roles viz-id])]
        ^{:key (str (random-uuid))}
        [:option {:value -role} -role])]
     [:button {:type     "button"
               :on-click #(>evt [:toggle-sort-roles viz-id role])}
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
     [:h3 "Select Roles "]
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
  [viz-id width]
  [flex-div width "50vh"
   [role-selection viz-id]
   [dt-views/data-table
    [:visible-mops viz-id]
    (into [{:col-key [:id]}
           {:col-key [:name]}]
          (slot-cols viz-id))
    {:header      (role-aggregation-row viz-id)
     :row-options (fn [i]
                    {:on-click #(>evt [:toggle-selected-mop viz-id i])
                     :style    {:fill             "blue"
                                :background-color (<sub [:mop-color viz-id i])}})}]])

(defn ajax-test-btn
  "Test if ajax sent and received"
  []
  (let [do-action #(>evt [:request-mop (<sub [:request])])]
    [:div
     [:input
      {:type         "text"
       :value        (<sub [:request])
       :on-change    #(>evt [:set-request (util/target-value %)])
       :on-key-press #(when (= (.-charCode %) 13)
                        (do-action))}]
     [:button
      {:type     "button"
       :on-click do-action}
      "Send message"]
     [:label (<sub [:cached-data])]]))

(defn main-panel []
  (let [width "30%"]
    [:div
     [:h1 (<sub [:name])]
     [ajax-test-btn]
     [:div
      [mop-table :panel1 width]
      [force-viz :force-viz1 width]]]))