(ns client.subs
  (:require [re-frame.core :as rf]
            [client.logging :refer [log]]))


(rf/reg-sub ::transaction (fn [db [_ id]] (get-in db [:transactions id])))

(rf/reg-sub ::visible-transactions (fn [db [_]] (:visible-transactions db)))

(rf/reg-sub ::is-selected (fn [db [_ id]] (not (nil? (some #{id} (:selected-transactions db))))))

(rf/reg-sub ::all-selected (fn [db [_]] (:all-selected db)))

;; (rf/reg-sub ::category-counter (fn [db [_]] 
;;                                  ((juxt #(->> %
;;                                               vals
;;                                               (map :category)
;;                                               (filter some?)
;;                                               count
;;                                               log) 
;;                                         count) 
;;                                   (:transactions db))))

(rf/reg-sub ::filter-term (fn [db [_]] (:filter-term db)))

(defn nil->num [x] (if (nil? x) 1 0))

(rf/reg-sub ::uncategorized (fn [db [_]] (->> (:visible-transactions db) (map (fn [id] (get (:transactions db) id))) (filter (comp nil? :category)))))

(rf/reg-sub ::n-uncategorized (fn [db [_]] (->> (:transactions db) vals (map (comp nil->num :category)) (reduce +))))

(rf/reg-sub ::n-categorized (fn [db [_]] (->> (:transactions db) vals (map :category) (filter some?) count)))

(rf/reg-sub ::single-assignment (fn [db [_]] (:single-assignment db)))