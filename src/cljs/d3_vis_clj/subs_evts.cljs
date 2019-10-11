(ns d3-vis-clj.subs-evts
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as rf :refer [subscribe reg-sub
                                          reg-event-db reg-event-fx
                                          trim-v path]]
            [d3.force-directed.subs-evts :as fses]
            [ajax.core :as ajax]
            [d3-vis-clj.db :as db]
            [clojure.set :as set]
            [data-table.db :as dt-db]
            [data-table.subs-evts :as dt-ses]
            [d3-vis-clj.util :as util]))

(reg-event-db :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db :window-resized
  [trim-v]
  (fn [db [new-width new-height]]
    (-> db
        (assoc-in [:height] new-height)
        (assoc-in [:width] new-width))))

(reg-event-fx :initialize-window-resize
  [trim-v]
  (fn [_ _]
    (let [init-width  js/window.innerWidth
          init-height js/window.innerHeight]
      {:window/on-resize {:dispatch [:window-resize]}
       :dispatch         [:window-resize init-width init-height]})))

(reg-sub :window-height
  (fn [db]
    (:height db)))

(reg-sub :window-width
  (fn [db]
    (:width db)))

(reg-event-db :toggle-selected-mop
  [trim-v (util/path-nth) (path [:data]) (util/path-nth) (path [:selected])]
  not)

(reg-sub :viz-type
  (fn [db [_ viz-id]]
    (get-in db [viz-id :type])))

(reg-sub :cached-mops
  (fn [db [_ id]]
    (get-in db [:cached-data :mops id])))

(reg-sub :node-to-add
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-to-add])))

(reg-event-db :set-node-to-add
  [trim-v (util/path-nth)]
  (fn [db [node-id]]
    (assoc-in db [:node-to-add] (keyword node-id))))

(reg-sub :mop
  (fn [[_ viz-id i] _]
    [(subscribe [:viz-type viz-id])
     (subscribe [::fses/node viz-id i])
     (subscribe [::dt-ses/row-value viz-id i])
     (subscribe [:cached-mops i])])
  (fn [[type node row mop] [_ _ _]]
    (case type
      ::dt-db/table row
      ::fses/force-layout node
      mop)))

(reg-event-db :toggle-sort-roles
  [trim-v (util/path-nth)]
  (fn [db [role]]
    (let [sorted? (get-in db [:sorted-roles role])]
      (-> db
          (update-in [:sorted-roles] #(if sorted?
                                        (disj % role)
                                        (conj (or % #{}) role)))
          (update-in [:data]
                     (fn [data]
                       (cond->> data
                                true (sort-by #(let [v (get-in % [:slots role])]
                                                 (if (coll? v)
                                                   (first v)
                                                   v)))
                                sorted? (reverse)
                                true (vec))))))))



(reg-sub :sorted-roles
  (fn [db [_ viz-id]]
    (get-in db [viz-id :sorted-roles])))

(reg-sub :sorted-role?
  (fn [[_ viz-id] _]
    (subscribe [:sorted-roles viz-id]))
  (fn [roles [_ _ role]]
    (contains? roles role)))

(reg-event-db :toggle-visible-role
  [trim-v (util/path-nth) (path [:visible-roles])]
  (fn [db [role]]
    (util/toggle-contains-vector (or db []) role)))

(reg-event-db :set-visible-role
  [trim-v (util/path-nth) (path [:visible-roles]) (util/path-nth)]
  (fn [_ [role]]
    role))

(reg-sub :visible-role?
  (fn [[_ viz-id] _]
    (subscribe [:visible-roles viz-id]))
  (fn [roles [_ _ role]]
    (some #(= role %) roles)))

(reg-event-db :add-visible-role
  [trim-v (util/path-nth) (path [:visible-roles])]
  (fn [db [role]]
    (conj (or db []) role)))

(reg-sub :visible-roles
  (fn [db [_ viz-id]]
    (get-in db [viz-id :visible-roles])))

(rf/reg-cofx :all-roles
  (fn [{[viz-id] :event :as coeffects}]
    (assoc coeffects
      :all-roles @(rf/subscribe [:all-roles viz-id])
      :all-roles-visible? @(rf/subscribe [:all-roles-visible? viz-id]))))

(reg-event-fx :toggle-all-roles
  [trim-v (rf/inject-cofx :all-roles) (util/path-nth) (path [:visible-roles])]
  (fn [{:keys [all-roles roles all-roles-visible?]} _]
    {:db (if all-roles-visible?
           []
           (->> roles
                (set)
                (set/difference all-roles)
                (into roles)))}))

(reg-sub :all-roles
  (fn [[_ viz-id] _]
    (subscribe [:visible-mops viz-id]))
  (fn [mops _]
    (->> mops
         (map :slots)
         (mapcat keys)
         (set))))

(reg-sub :all-roles-visible?
  (fn [[_ viz-id] _]
    [(subscribe [:all-roles viz-id])
     (subscribe [:visible-roles viz-id])])
  (fn [[all-roles visible-roles] _]
    (every? (set visible-roles) all-roles)))

(reg-event-db :toggle-node-labels
  [trim-v (util/path-nth) (rf/path [:node-config :show-labels])]
  not)

(reg-sub :node-labels?
  (fn [db [_ viz-id]]
    (get-in db [viz-id :node-config :show-labels])))

(reg-sub :node-label
  (fn [[_ viz-id i] _]
    [(subscribe [:node-labels? viz-id])
     (subscribe [::fses/node viz-id i])])
  (fn [[show-labels node] _]
    (when (or (:selected node)
              (:hovered node)
              show-labels)
      (:name node))))

(reg-event-db :toggle-link-labels
  [trim-v (util/path-nth) (path [:link-config :show-labels])]
  not)

(reg-sub :link-labels?
  (fn [db [_ viz-id]]
    (get-in db [viz-id :link-config :show-labels])))

(reg-sub :link-label
  (fn [[_ viz-id i] _]
    [(subscribe [:link-labels? viz-id])
     (subscribe [::fses/link viz-id i])])
  (fn [[show-labels link] _]
    (when (or (:hovered link)
              show-labels)
      (:label link))))

(reg-sub :name
  (fn [db]
    (:name db)))

(reg-sub :hierarchy
  (fn [db]
    (get-in db [:cached-data :hierarchy])))

(reg-sub :visible-mops
  (fn [db [_ viz-id]]
    (get-in db [viz-id :data])))

(def selected-color "red")

(reg-sub :mop-color
  (fn [[_ viz-id i] _]
    [(subscribe [:hierarchy]) (subscribe [:mop viz-id i])])
  (fn [[h {:keys [id hovered selected]}] _]
    (cond hovered "yellow"
          selected selected-color
          (isa? h id :A) "cyan"
          (isa? h id :B) "pink"
          :default "white")))

(reg-sub :node-stroke
  (fn [[_ viz-id i]]
    (subscribe [::fses/node viz-id i]))
  (fn [node _]
    (if (:selected node)
      selected-color
      "white")))

(reg-sub :link-stroke
  (fn [[_ viz-id i]]
    (subscribe [::fses/link viz-id i]))
  (fn [link _]
    (if (:selected link)
      selected-color
      "#E5E5E5")))

(reg-sub :all-mops
  (fn [db _]
    (vals (get-in db [:cached-data :mops]))))

(reg-event-fx :request-mop
  [trim-v]
  (fn [{:keys [db]} [id]]
    {:db         (assoc db :sending true)
     :http-xhrio {:method          :get
                  :uri             "/mop/"
                  :params          {:id id}
                  :timeout         3000
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:receive-message]
                  :on-failure      [:handle-failure]}}))

(reg-event-db :receive-message
  [trim-v]
  (fn [db [response]]
    (println "success" response)
    (assoc db :response response
              :sending false)))

(reg-event-db :handle-failure
  [trim-v]
  (fn [db [response]]
    (println "failure" response)
    (assoc db :response :failure
              :sending false)))

(reg-sub :response
  (fn [db _]
    (str (get db :response))))

(reg-event-db :set-request
  [trim-v]
  (fn [db [id]]
    (assoc db :request id)))

(reg-sub :request
  (fn [db _]
    (str (get db :request))))

