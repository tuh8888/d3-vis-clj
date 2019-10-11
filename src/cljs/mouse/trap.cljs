(ns mouse.trap
  (:require [clojure.string :as str]
            [cljsjs.mousetrap]))

(defn join-keys
  "Joins keys by +"
  [keys]
  (->> keys
       (map name)
       (str/join "+")))

(defn bind
  ([keys f]
   (.bind js/Mousetrap (join-keys keys) f))
  ([selector keys f]
   (-> js/document
       (.querySelector selector)
       (js/Mousetrap.)
       (.bind (join-keys keys) f))))
