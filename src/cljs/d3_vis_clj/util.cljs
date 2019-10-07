(ns d3-vis-clj.util
  (:require [goog.object :as gobj]))

(defn get-label
  [d]
  (gobj/get d "label"))

(defn get-value [d]
  (gobj/get d "value"))

(defn get-id [d]
  (gobj/get d "id"))

(defn text-value
  "Returns value found in text field"
  [d]
  (-> d (.-target ) (.-value)))

(defn set-ons
  "Utility function for adding a bunch of ons."
  [node & {:as ons}]
  (reduce (fn [node [on on-fn]]
            (.on node (name on) on-fn))
          node ons))

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)