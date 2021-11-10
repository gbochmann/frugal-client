(ns client.events
  (:require [re-frame.core :as rf]
            [client.db :refer [init-db]]
            [client.logging :refer [log]]))

(rf/reg-event-db ::initialize (fn [ _ _ ] init-db))

(rf/reg-event-db 
 
 ::add-transaction 
 
 (fn [db [_ t]] (-> db 
                    (assoc-in [:transactions (:uuid t)] t)
                    (assoc :visible-transactions (conj (:visible-transactions db) (:uuid t))))))


(rf/reg-event-db

 ::select-transaction

 (fn [db [_ id]]
   (if
    (nil? (some #{id} (:selected-transactions db)))

     (let [new-db (assoc db :selected-transactions (conj (:selected-transactions db) id))]
       (if
        (= (count (:visible-transactions new-db)) (count (:selected-transactions new-db)))
         (assoc new-db :all-selected true)
         new-db))

     (-> db (assoc :selected-transactions (remove #{id} (:selected-transactions db))) (assoc :all-selected false)))))


(rf/reg-event-db

 ::assign-category

 (fn [db [_]] (reduce (fn [d id] (update-in d [:transactions id :category] (fn [_] (:category-value db)))) db (:selected-transactions db))))


(rf/reg-event-db

 ::category-value

 (fn [db [_ text]] (assoc db :category-value text)))


(rf/reg-event-db

 ::select-all-visible

 (fn [db [_]] (log "select all visible")
   (if
    (:all-selected db)
     (-> db (assoc :selected-transactions []) (assoc :all-selected false))
     (-> db (assoc :selected-transactions (:visible-transactions db)) (assoc :all-selected true)))))


(rf/reg-event-db
 
 ::filter-table
 
 (fn [db [_ term]] (assoc db :filter-term term)))


(rf/reg-event-db
 
 ::clear-filter
 
 (fn [db [_]] (-> db (assoc :filter-term nil) (assoc :visible-transactions (map :uuid (:transactions db))))))