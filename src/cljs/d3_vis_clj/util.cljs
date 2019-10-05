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