(ns data-table.views
  (:require [d3-vis-clj.util :refer [<sub]]))

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
