(ns client.events
  (:require [re-frame.core :as rf]
            [client.db :refer [init-db]]
            [client.logging :refer [log]]))

(rf/reg-event-db ::initialize (fn [ _ _ ] init-db))

(rf/reg-event-db 
 
 ::add-transaction 
 
 (fn [db [_ t]] (assoc-in db [:transactions (:uuid t)] t)))


(rf/reg-event-db 
 
 ::select-transaction
 
 (fn [db [_ id]]
   (update-in db [:transactions id :selected] not)))