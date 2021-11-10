(ns client.subs
  (:require [re-frame.core :as rf]
            [client.logging :refer [log]]))


(rf/reg-sub ::transaction (fn [db [_ id]] (get-in db [:transactions id])))

(rf/reg-sub ::visible-transactions (fn [db [_]] (:visible-transactions db)))

(rf/reg-sub ::is-selected (fn [db [_ id]] (not (nil? (some #{id} (:selected-transactions db))))))

(rf/reg-sub ::all-selected (fn [db [_]] (:all-selected db)))