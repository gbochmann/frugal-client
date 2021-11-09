(ns client.subs
  (:require [re-frame.core :as rf]
            [client.logging :refer [log]]))

(rf/reg-sub ::all-transactions (fn [db _] (log db) (:transactions db)))

(rf/reg-sub ::transaction (fn [db [_ id]] (get-in db [:transactions id])))

(rf/reg-sub ::visible-transactions (fn [db [_]] (:visible-transactions db)))
