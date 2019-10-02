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

(rf/reg-sub :db
            (fn [db _]
              db))