(ns d3.force-directed.util)

(defn translate
  [x y]
  (str "translate(" x "," y ")"))

(defn coord
  [d x-y]
  (case x-y
    :x (.-x d)
    :y (.-y d)))

(defn link-endpoint
  [link end]
  (case end
    :source (.-source link)
    :target (.-target link)))

(defn link-midpoint
  [link]
  (let [source (link-endpoint link :source)
        target (link-endpoint link :target)
        sx     (coord source :x)
        sy     (coord source :y)
        tx     (coord target :x)
        ty     (coord target :y)
        dx     (- tx sx)
        dy     (- ty sy)
        dist   (Math/hypot dx dy)]
    [(+ (/ dx dist)
        (/ (+ sx tx) 2))
     (+ (/ dy dist)
        (/ (+ sy ty) 2))]))

(defn event-active?
  []
  (-> js/d3
      (.-event)
      (.-active)
      (zero?)
      (not)))

(defn get-nodes
  [sim]
  (.nodes sim))

(defn get-node
  ([sim i]
   (-> sim (get-nodes) (get i)))
  ([sim i c]
   (-> sim (get-node i) (coord c))))

(defn get-links
  [sim]
  (-> sim
      (.force "link")
      (.links)))

(defn get-link
  ([sim i]
   (-> sim (get-links) (get i)))
  ([sim i end]
   (-> sim (get-link i) (link-endpoint end)))
  ([sim i end c]
   (-> sim (get-link i end) (coord c))))

(defn set-alpha-target!
  [sim alpha-target]
  (.alphaTarget sim alpha-target))
