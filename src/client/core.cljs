(ns client.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :refer [dispatch-sync dispatch subscribe]]
            [client.csv :refer [csvstr->map ing->transaction]]
            [client.logging :refer [log]]
            [client.events :as events]
            [client.subs :as subs]))


(defn read-file [onload-callback event]
  (let [file (-> event .-target .-files (aget 0))
        r (js/FileReader.)]

    (set! r -onload onload-callback)

    (.readAsText r file)))

(defn dispatch-transaction [t] (dispatch [::events/add-transaction t]) t)

(defn add-uuid [t] (assoc t :uuid (random-uuid)))

(def transformations (comp add-uuid ing->transaction))

(defn transform-csv [event] (-> event
                                .-target
                                .-result
                                csvstr->map
                                (->> (map transformations) (map dispatch-transaction) doall)))

(defn file-input []
  [:div.block
   [:div.block [:label {:for "csv-input"} "Add CSV file"]]
   [:div.block
    [:input#csv-input {:type "file"
                       :name "csv-input"
                       :accept "text/csv"
                       :on-change (partial read-file transform-csv)}]]])

(defn transaction-checkbox [id]
  [:input {:type "checkbox"
           :on-change #(dispatch [::events/select-transaction id])}])

(defn transaction-row [{:keys [uuid date note category income expense] :as t}]
  [:tr [:td (transaction-checkbox uuid)] [:td date] [:td note] [:td category] [:td income] [:td expense]])

(defn transactions-table [transactions]
  [:table.table
   [:thead [:tr [:th [:input {:type "checkbox"}]] [:th "Date"] [:th "Note"] [:th "Category"] [:th "Income"] [:th "Expense"]]]
   [:tbody (doall (for [t transactions] (transaction-row @(subscribe [::subs/transaction t]))))]])

(defn transaction-list
  []
  (let [transactions @(subscribe [::subs/visible-transactions])]
    [:div.block (when (< 0 (count transactions)) (log "rendering transaction list") (transactions-table transactions))]))

(defn category-input
  []
  [:div.columns
   [:div.column.is-four-fifths
    [:label.mr-3 {:for "category-input"} "Category:"]
    [:input {:type "text" :on-change #(dispatch [::events/category-value (-> % .-target .-value)])}]]
   [:div.column.is-one-fifth
    [:input {:type "button" :value "Assign Category" :on-click (fn [] (dispatch [::events/assign-category]))}]]])

(defn views [] [:div.container
                [:div.columns
                 [:div.column.is-one-third [file-input]]
                 [:div.column.is-two-thirds [category-input]]]
                [transaction-list]])

(defn
  ^:export

  ;; This triggers a page reload once the code has been reloaded. See https://code.thheller.com/blog/shadow-cljs/2019/08/25/hot-reload-in-clojurescript.html
  ^:dev/after-load

  main []

  (dispatch-sync [::events/initialize])

  (rdom/render [views] (js/document.getElementById "app")))