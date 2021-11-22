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

(defn init-fields [t] (assoc t :selected false :category nil))

(def transformations (comp add-uuid init-fields ing->transaction))

(defn transform-csv [event] (-> event
                                .-target
                                .-result
                                csvstr->map
                                (->> (map transformations) (map dispatch-transaction) doall)))

(defn file-input
  []
  [:input#csv-input.button
   {:type "file"
    :name "csv-input"
    :accept "text/csv"
    :on-change (partial read-file transform-csv)}])

(defn transaction-checkbox
  [id]
  (let [is-checked @(subscribe [::subs/is-selected id])]
    [:input {:type "checkbox"
             :checked is-checked
             :on-change #(dispatch [::events/select-transaction id])}]))

(defn transaction-row
  [{:keys [uuid date note category income expense]}]
  (with-meta [:tr [:td (transaction-checkbox uuid)] [:td date] [:td note] [:td category] [:td income] [:td expense]] {:key uuid}))

(defn transactions-table
  [transactions]
  (let [all-selected @(subscribe [::subs/all-selected])]
    [:table.table
     [:thead [:tr [:th [:input {:type "checkbox" :on-change #(dispatch [::events/select-all-visible]) :checked all-selected}]] [:th "Date"] [:th "Note"] [:th "Category"] [:th "Income"] [:th "Expense"]]]
     [:tbody (doall (for [t transactions] (transaction-row @(subscribe [::subs/transaction t]))))]]))

(defn transaction-list
  []
  (let [transactions @(subscribe [::subs/visible-transactions])]
    [:div.block (when (< 0 (count transactions)) (log "rendering transaction list") (transactions-table transactions))]))

(defn category-input
  []
  [:div.field.has-addons
   [:div.control.is-expanded
    [:input.input {:type "text" :on-change #(dispatch [::events/category-value (-> % .-target .-value)]) :placeholder "Enter category"}]]
   [:div.control [:button.button.is-info {:on-click (fn [] (dispatch [::events/assign-category]))} "Assign"]]])

(defn filter-input
  []
  [:div.field.has-addons
   [:div.control.is-expanded
    [:input.input {:type "text" :on-change #(dispatch-sync [::events/filter-table (-> % .-target .-value)]) :placeholder "Enter filter term"}]]
   [:div.control [:button.button {:on-click (fn [] (dispatch [::events/clear-filter]))} "Clear"]]])

(defn export-button
  []
  [:div.field
   [:button.button.is-primary {:on-click (fn [] (dispatch [::events/export-csv]))} "Export"]])

(defn export-dropdown
  []
  [:div.dropdown.is-hoverable
   [:div.dropdown-trigger
    [:button.button {:aria-haspopup true :aria-controls "dropdown-menu"}
     [:span "Export"]
     [:span.icon.is-small
      [:ion-icon {:name "chevron-down-outline"}]]]]
   [:div#dropdown-menu.dropdown-menu {:role "menu"}
    [:div.dropdown-content
     [:div.dropdown-item
      [:button {:on-click #(dispatch [::events/export-csv])} "Transactions"]]
     [:div.dropdown-item
      [:button {:on-click #(dispatch [::events/export-category-csv])} "Totals by category"]]]]])

(defn category-counter
  []
  (let [[categorized total] @(subscribe [::subs/category-counter])] 
    [:div.container [:progress.progress.is-primary {:value categorized :max total} (str categorized "/" total)]]))

(defn views []
  [:div.container
   [:div.columns
    [:div.column.is-one-ffith [file-input]]
    [:div.column.is-one-fifth [export-dropdown]]
    [:div.column.is-one-fifth [filter-input]]
    [:div.column.is-one-fifth [category-input]]
    [:div.column.is-one-fifth [category-counter]]]
   [transaction-list]])

(defn
  
  main []

  (dispatch-sync [::events/initialize])

  (rdom/render [views] (js/document.getElementById "app")))

(main)