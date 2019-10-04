(ns d3-vis-clj.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :name
 (fn [db]
   (:name db)))

(rf/reg-sub :global-width
            (fn [db _]
              (:width db)))

(rf/reg-sub
  :data
  (fn [db]
    (:test-data db)))

(rf/reg-sub
  :get-var
  (fn [db [_ var]]
    (get-in db [:test-data :dataset var])))

(rf/reg-sub :node-size
  (fn [db]
    (get-in db [:test-data :node-config :r])))