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

 (fn [db [_ id]] (assoc db :selected-transactions (conj (:selected-transactions db) id))))


(rf/reg-event-db

 ::assign-category

 (fn [db [_]] (reduce (fn [d id] (update-in d [:transactions id :category] (fn [_] (:category-value db)))) db (:selected-transactions db))))


(rf/reg-event-db

 ::category-value

 (fn [db [_ text]] (assoc db :category-value text)))