(ns client.fx
  (:require [re-frame.core :as rf]
            [client.db :refer [init-db]]
            [client.logging :refer [log]]))

(rf/reg-event-db :initialize (fn [ _ _ ] init-db))

(rf/reg-event-db 
 
 :add-transaction 
 
 (fn [db [_ t]] 
   (log t)
   (assoc db (:uuid t) t)))