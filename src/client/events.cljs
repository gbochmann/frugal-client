(ns client.events
  (:require [re-frame.core :as rf]
            [client.db :refer [init-db]]
            [client.logging :refer [log]]
            [clojure.string :refer [includes? blank? lower-case trim join split]]
            [client.fx :as fx]
            [client.csv :refer [csvstr->map fidor->transaction ing->transaction]]))

(rf/reg-event-db ::initialize (fn [_ _] init-db))

(rf/reg-event-db

 ::assign-category

 (fn [db [_]] (reduce 
               (fn [d id] 
                 (update-in d 
                            [:transactions id :category] 
                            (fn [_] (:category-value db)))) 
               db 
               (:visible-transactions db))))


(rf/reg-event-db

 ::category-value

 (fn [db [_ text]] (assoc db :category-value text)))


(defn filter-by-note [term transactions]
  (if
   (blank? term)
    (keys transactions)
    (->> transactions (filter #(includes? (lower-case (get-in % [1 :note])) (lower-case (trim term)))) (map first))))


(rf/reg-event-db

 ::filter-table

 (fn
   [db [_ term]]

   (let [new-visible-ts (filter-by-note term (:transactions db))]

     (-> db
         (assoc :filter-term term)
         (assoc :visible-transactions new-visible-ts)))))


(defn clear-filter [db [_]] (-> db (assoc :filter-term nil) (assoc :visible-transactions (keys (:transactions db)))))
(rf/reg-event-db ::clear-filter clear-filter)


(defn map->csv
  [transactions headers]
  (->> transactions
       (map second)
       (map #(dissoc % :uuid :selected))
       (map #(into [] %))
       (reduce (fn [csv items] (str csv (join ";" (map second items)) "\n")) (str (join ";" headers) "\n"))))


(rf/reg-event-fx

 ::export-csv

 (fn [{:keys [db]} _] {::fx/export-csv (map->csv (:transactions db) ["Date" "Note" "Expense" "Income" "Category"])}))

(def month-map
  {"01" "January"
   "02" "February"
   "03" "March"
   "04" "April"
   "05" "May"
   "06" "June"
   "07" "July"
   "08" "August"
   "09" "September"
   "10" "October"
   "11" "November"
   "12" "December"})

(defn get-month [date] (->> (split date #"\.") rest vec))

(defn sum-by-category
  [acc transaction]
  (update-in acc [(get-month (:date transaction)) (:category transaction)] (fn [expenses] (+ expenses (:expense transaction)))))

(defn transactions->category-sum
  [transactions]
  (->> transactions vals (filter #(not (nil? (:category %)))) (reduce sum-by-category {})))

(defn sum-by-cat->csv
  [categories]
  (fn [acc [month sum]]
    (conj acc (str (->> month ((juxt second #(month-map (first %)))) (join ";"))
                   ";"
                   (join ";"
                         (map (fn [c] (let [exp (get sum c)] (if exp exp 0))) categories))
                   "\n"))))

(defn category-sum->csv
  [sums]
  (let [categories (->> sums vals (map keys) flatten  (into #{}))]
    (apply str
           (->> sums
                (reduce (sum-by-cat->csv categories) [])
                (into ["Year;Month;" (join ";" categories) "\n"])))))

(rf/reg-event-fx

 ::export-category-csv

 (fn [{:keys [db]} _] {::fx/export-category-csv (-> (:transactions db)
                                                    transactions->category-sum
                                                    category-sum->csv)}))

(defn init-fields [t] (assoc t :selected false :category nil))
(defn add-uuid [t] (assoc t :uuid (random-uuid)))
(def base-transformations [add-uuid init-fields])

(defn add-transaction 
  [db t] 
  (-> db
      (assoc-in [:transactions (:uuid t)] t)
      (assoc :visible-transactions (conj (:visible-transactions db) (:uuid t)))))

(defn make-transaction-event [transformation]
  (fn [db [_ event]]
    (let [transactions (->> event .-target .-result csvstr->map (map (apply comp (conj base-transformations transformation))))]
      (reduce add-transaction db transactions))))

(def add-fidor-transactions (make-transaction-event fidor->transaction))
(rf/reg-event-db ::add-fidor-transactions add-fidor-transactions)

(def add-ing-transactions (make-transaction-event ing->transaction))
(rf/reg-event-db ::add-ing-transactions add-ing-transactions)

(rf/reg-event-db ::toggle-single-assignment (fn [db _] (assoc db :single-assignment (not (:single-assignment db)))))