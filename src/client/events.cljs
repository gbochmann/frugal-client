(ns client.events
  (:require [re-frame.core :as rf]
            [client.db :refer [init-db]]
            [client.logging :refer [log]]
            [clojure.string :refer [includes? blank? lower-case trim join split]]
            [client.fx :as fx]
            [client.csv :refer [csvstr->map fidor->transaction ing->transaction]]
            [cljs-time.core :refer [local-date after?]]))

(defn initialize [_ _] init-db)
(rf/reg-event-db ::initialize initialize)

(defn add-single-category
  [db id]
  (-> db
      (update-in [:transactions id :category] (fn [_] (:category-value db)))
      (update :uncategorized-transactions (fn [uncategorized] (remove #{id} uncategorized)))
      (update-in [:categorized-transactions] (fn [categorized] (conj categorized id)))))

(defn add-bulk-category
  [db [_]] (reduce add-single-category db (:uncategorized-transactions db)))

(rf/reg-event-db ::assign-bulk-category add-bulk-category)

(defn assign-single-category [db [_]] (add-single-category db (first (:uncategorized-transactions db))))
(rf/reg-event-db ::assign-single-category assign-single-category)

(defn category-value [db [_ text]] (assoc db :category-value text))
(rf/reg-event-db ::category-value category-value)


(defn transaction->local-date
  [t]
  (->> t :date reverse (apply local-date)))

(defn compare-transaction-dates
  [t1 t2]
  (after? (transaction->local-date t1)
          (transaction->local-date t2)))


(defn filter-by-note [term transactions]
  (if
   (blank? term)
    transactions
    (->> transactions (filter #(includes?
                                (lower-case (get % :note))
                                (lower-case (trim term)))))))


(defn filter-table
  " Takes all transactions from the db
  filters it by term and updates the list of visible-transactions
  in the db."

  [db [_ term]]

  (let [new-visible-ts (->> db
                            :transactions
                            vals
                            (filter-by-note term)
                            (sort compare-transaction-dates)
                            (map :uuid))
        is-single-assignment (= 0 (count term))]

    (-> db
        (assoc :filter-term term)
        (assoc :uncategorized-transactions new-visible-ts)
        (assoc :single-assignment is-single-assignment))))

(rf/reg-event-db ::filter-table filter-table)


(defn clear-filter [db [_]] (-> db
                                (assoc :filter-term nil)
                                (assoc :uncategorized-transactions (map :uuid (sort compare-transaction-dates (filter (comp nil? :category) (vals (:transactions db))))))
                                (assoc :single-assignment true)))

(rf/reg-event-db ::clear-filter clear-filter)


(defn map->csv
  [transactions headers]
  (->> transactions
       (map second)
       (map #(dissoc % :uuid :selected))
       (map #(into [] %))
       (reduce (fn [csv items] (str csv (join ";" (map second items)) "\n")) (str (join ";" headers) "\n"))))

(defn export-csv [{:keys [db]} _] {::fx/export-csv (map->csv (:transactions db) ["Date" "Note" "Expense" "Income" "Category"])})
(rf/reg-event-fx ::export-csv export-csv)

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
  [transactions t]
  (update-in transactions [(get-month (:date t)) (:category t)] (fn [expenses] (+ expenses (:expense t)))))

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

(defn export-category-csv [{:keys [db]} _] {::fx/export-category-csv (-> (:transactions db)
                                                   transactions->category-sum
                                                   category-sum->csv)})
(rf/reg-event-fx ::export-category-csv export-category-csv)

(defn init-fields [t] (assoc t :selected false :category nil))
(defn add-uuid [t] (assoc t :uuid (random-uuid)))
(def base-transformations [add-uuid init-fields])

(defn add-transaction
  [db t]
  (-> db
      (assoc-in [:transactions (:uuid t)] t)
      (assoc :uncategorized-transactions (conj (:uncategorized-transactions db) (:uuid t)))))

(defn make-transaction-event [transformation]
  (fn [db [_ event]]
    (let [transactions (->> event .-target .-result csvstr->map
                            (map (apply comp (conj base-transformations transformation))))]
      (reduce add-transaction db (sort compare-transaction-dates transactions)))))

(def add-fidor-transactions (make-transaction-event fidor->transaction))
(rf/reg-event-db ::add-fidor-transactions add-fidor-transactions)

(def add-ing-transactions (make-transaction-event ing->transaction))
(rf/reg-event-db ::add-ing-transactions add-ing-transactions)

(rf/reg-event-db ::toggle-single-assignment (fn [db _] (assoc db :single-assignment (not (:single-assignment db)))))

(rf/reg-event-db ::switch-view (fn [db [_ view]] (assoc db :main-view view)))