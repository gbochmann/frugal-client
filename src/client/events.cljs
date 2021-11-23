(ns client.events
  (:require [re-frame.core :as rf]
            [client.db :refer [init-db]]
            [client.logging :refer [log]]
            [clojure.string :refer [includes? blank? lower-case trim join split]]
            [client.fx :as fx]
            [client.csv :refer [csvstr->map fidor->transaction]]))

(rf/reg-event-db ::initialize (fn [_ _] init-db))

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


(defn filter-by-note [term transactions]
  (if
   (blank? term)
    (keys transactions)
    (->> transactions (filter #(includes? (lower-case (get-in % [1 :note])) (lower-case (trim term)))) (map first))))


(rf/reg-event-db

 ::filter-table

 (fn
   [db [_ term]]

   (let [new-visible-ts (filter-by-note term (:transactions db))
         all-ts-selected (= (count (:selected-transactions db)) (count new-visible-ts))]

     (-> db
         (assoc :filter-term term)
         (assoc :visible-transactions new-visible-ts)
         (assoc :all-selected all-ts-selected)))))


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
                                                    ;; log
                                                    category-sum->csv)}))

(defn init-fields [t] (assoc t :selected false :category nil))
(defn add-uuid [t] (assoc t :uuid (random-uuid)))
(def base-transformations [add-uuid init-fields])

;; (doall (map #(update db :transactions (fn [old-ts] (assoc old-ts (:uuid %) %))) transactions))

(defn add-transaction
  [db t]
  (update db :transactions (fn [old-ts] (assoc old-ts (:uuid t) t) )))

(defn add-fidor-transactions
  [[db [_ event]]] (let [transactions (->> event .-target .-result csvstr->map (map (apply comp (conj base-transformations fidor->transaction))))]
                     (reduce add-transaction db transactions)))

(rf/reg-event-db ::add-fidor-transaction add-fidor-transactions)