(ns d3-vis-clj.util
  (:require [cljsjs.d3]
            [goog.object :as gobj]
            [re-frame.core :refer [->interceptor
                                   get-coeffect assoc-coeffect
                                   get-effect assoc-effect]]))

(defn win-inner-w
  []
  js/window.innerWidth)

(defn win-inner-h
  []
  js/window.innerHeight)

(def key-map {:enter 13
              :ctrl  17})

(def d3-key-map
  {:ctrl ctrlKey})

(defn key?
  [e key]
  (if (= :d3 e)
    (key d3-key-map)
    (= (.-charCode e) (key key-map))))

(defn get-label
  [d]
  (gobj/get d "label"))

(defn get-value [d]
  (gobj/get d "value"))

(defn get-id [d]
  (gobj/get d "id"))

(defn target-value
  "Returns value found in text field"
  [d]
  (-> d (.-target) (.-value)))

(defn set-ons
  "Utility function for adding a bunch of ons."
  [node ons]
  (reduce (fn [node [on on-fn]]
            (.on node (name on) on-fn))
          node ons))

(def <sub (comp deref re-frame.core/subscribe))
(def >evt re-frame.core/dispatch)

(defn remove-nth
  [v n]
  (-> v
      (subvec 0 n)
      (concat (subvec v (inc n)))
      (vec)))

(defn path-nth
  ([]
   (path-nth 0))
  ([i]
   (let [db-store-key         :re-frame-path/db-store
         first-path-store-key (keyword *ns* (str (name ::first-path-store)
                                                 (random-uuid)))]
     (->interceptor
       :id :viz-id-path
       :before (fn [context]
                 (let [original-db (get-coeffect context :db)
                       viz-id      (get-in context [:coeffects :event i])
                       new-db      (get original-db viz-id)]
                   (-> context
                       (update-in [:coeffects :event] remove-nth i)
                       (update db-store-key conj original-db)
                       (assoc first-path-store-key viz-id)
                       (assoc-coeffect :db new-db))))

       :after (fn [context]
                (let [db-store     (get context db-store-key)
                      original-db  (peek db-store)
                      new-db-store (pop db-store)
                      viz-id       (get context first-path-store-key)
                      context'     (-> context
                                       (assoc db-store-key new-db-store)
                                       (assoc-coeffect :db original-db)) ;; put the original db back so that things like debug work later on
                      db           (get-effect context :db ::not-found)]
                  (if (= db ::not-found)
                    context'
                    (->> db
                         (assoc original-db viz-id)
                         (assoc-effect context' :db)))))))))

(defn toggle-contains-set
  [coll x]
  (if (contains? coll x)
    (disj coll x)
    (conj (or coll #{}) x)))

(defn toggle-contains-vector
  [coll x]
  (if (some #(= x %) coll)
    (->> coll
         (remove #(= x %))
         (vec))
    (conj (or coll []) x)))