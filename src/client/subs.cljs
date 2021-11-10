(ns client.subs
  (:require [re-frame.core :as rf]
            [client.logging :refer [log]]
            [clojure.string :refer [includes? blank?]]))


(rf/reg-sub ::transaction (fn [db [_ id]] (get-in db [:transactions id])))


(rf/reg-sub 
 
 ::visible-transactions 
 
;; (if (or (nil? (:filter-term db)) (blank? (:filter-term db)))
;;   (:visible-transactions db)
;;   (->> (:transactions db)
;;        (filter (fn [t] (log t) (includes? (:note t) (:filter-term db))))
;;        (map :uuid)))

 (fn [db [_]]
   
   (if 
    (blank? (:filter-term db)) 
     (:visible-transactions db) 
     (->> (:transactions db) (filter #(includes? (get-in % [1 :note]) (:filter-term db))) (map first)))))


(rf/reg-sub ::is-selected (fn [db [_ id]] (not (nil? (some #{id} (:selected-transactions db))))))

(rf/reg-sub ::all-selected (fn [db [_]] (:all-selected db)))