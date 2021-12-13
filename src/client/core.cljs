(ns client.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :refer [dispatch-sync dispatch subscribe]]
            [client.events :as events]
            [client.subs :as subs]))


(defn read-file [onload-callback event]
  (let [file (-> event .-target .-files (aget 0))
        r (js/FileReader.)]

    (set! r -onload onload-callback)

    (.readAsText r file)))

(defn file-input
  []
  [:input#csv-input.button
   {:type "file"
    :name "csv-input"
    :accept "text/csv"
    :on-change (partial read-file #(dispatch [::events/add-ing-transactions %]))}])

(defn transaction-checkbox
  [id]
  (let [is-checked @(subscribe [::subs/is-selected id])]
    [:input {:type "checkbox"
             :checked is-checked
             :on-change #(dispatch [::events/select-transaction id])}]))

(defn transaction-row
  [{:keys [uuid date note category income expense]}]
  (with-meta [:tr [:td (transaction-checkbox uuid)] [:td date] [:td note] [:td category] [:td income] [:td expense]] {:key uuid}))

(defn category-input
  []
  [:div.flex-container
   [:input.input.has-suffix.flex-grow-max {:type "text" :on-change #(dispatch [::events/category-value (-> % .-target .-value)]) :placeholder "Enter category"}]
   [:button.button.suffix.submit {:on-click (fn [] (dispatch [::events/assign-category]))} "Assign"]])

(defn filter-input
  []
  (let [filter-term @(subscribe [::subs/filter-term])]
    [:div.flex-container
     [:input.input.has-suffix.flex-grow-max {:type "text"
              :value filter-term
              :on-change #(dispatch-sync [::events/filter-table (-> % .-target .-value)]) :placeholder "Filter transactions by note"}]
     [:button.button.suffix.within-input {:on-click (fn [] (dispatch [::events/clear-filter]))} [:span.icon.fas.fa-times-circle]]]))

(defn export-dropdown
  []
  [:div
   [:div
    [:button {:aria-haspopup true :aria-controls "dropdown-menu"}
     [:span "Export"]
     [:span
      [:ion-icon {:name "chevron-down-outline"}]]]]
   [:div#dropdown-menu {:role "menu"}
    [:div
     [:div
      [:button {:on-click #(dispatch [::events/export-csv])} "Transactions"]]
     [:div
      [:button {:on-click #(dispatch [::events/export-category-csv])} "Totals by category"]]]]])

(defn transaction-card
  [uuid]
  (let [{:keys [date note category income expense]} @(subscribe [::subs/transaction uuid])]
    (with-meta 
      [:li.card-container
       [:div.card.box
        [:div.flex-grow-1 [:p (transaction-checkbox uuid)]]
        [:div.flex-grow-1 [:p date]]
        [:div.transaction-note [:p note] [:div.flex-grow-1 category]]
        [:div.flex-grow-1 [:p income] [:p expense]]]]
      {:key uuid}))
  )

(defn uncategorized
  []
  [:div.transaction-area
   [:section.transaction-list
    [:div.transaction-list-main
     [:div.box.sharp-bottom.top-bar [filter-input] [category-input]]
     [:ul (let [visibles @(subscribe [::subs/visible-transactions])]
            (doall (for [v visibles] (transaction-card v))))]]
    [:div.transaction-list-sidebar
     [file-input]]]
   ])

(defn views []
  [:div.with-sidebar
   [:nav.box.sidenav
    [:ul.nav-menu 
     [:li [:a.menu-link {:href "javascript:void"} [:p.menu-label [:span.icon.fas.fa-clipboard-list] "Uncategorized"] [:span.badge-container [:span.badge @(subscribe [::subs/n-uncategorized])]]]] 
     [:li [:a.menu-link {:href "javascript:void"} [:p.menu-label [:span.icon.fas.fa-clipboard-check] "Categorized"] [:span.badge-container [:span.badge @(subscribe [::subs/n-categorized])]]]]]]
   [:main.main [uncategorized]]])


(defn
  
  main []

  (dispatch-sync [::events/initialize])

  (rdom/render [views] (js/document.getElementById "app")))

(main)