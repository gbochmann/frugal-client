(ns client.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :refer [dispatch-sync dispatch subscribe]]
            [client.events :as events]
            [client.subs :as subs]
            [clojure.string :as string]
            [client.logging :refer [log]]))


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

(defn dispatch-category-input
  []
  (let [is-single-input @(subscribe [::subs/single-assignment])]

    (dispatch [(if is-single-input 
                 ::events/assign-single-category 
                 ::events/assign-bulk-category)])))

(defn category-input
  []
  [:div.flex-container
   [:input.input.has-suffix.flex-grow-max {:type "text" :on-change #(dispatch [::events/category-value (-> % .-target .-value)]) :placeholder "Enter category"}]
   [:button.button.suffix.submit {:on-click dispatch-category-input} "Assign"]])

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
  [{:keys [date note category income expense uuid]}]
  (with-meta 
      [:li.card-container
       [:div.card.box
        [:div.flex-grow-1 [:p (string/join "." date)]]
        [:div.transaction-note [:p note] [:div.flex-grow-1 category]]
        [:div.flex-grow-1 [:p income] [:p expense]]]]
      {:key uuid})
  )

(defn transaction-list-sidebar
  []
  [:div
   [:div
    (let [mode @(subscribe [::subs/single-assignment])] 
      [:input#single {:type "checkbox" :value mode :on-change (fn [] (dispatch [::events/toggle-single-assignment]))}])
    [:label {:for "single"} "Single"]]
   [file-input]])

(defn uncategorized
  []
  [:div.transaction-area
   [:section.transaction-list
    [:div.transaction-list-main
     [:div.box.sharp-bottom.top-bar [filter-input] [category-input]]
     [:ul (let [visibles @(subscribe [::subs/uncategorized])]
            (doall (for [v visibles] (transaction-card v))))]]
    [:div.transaction-list-sidebar
     [transaction-list-sidebar]]]
   ])

(defn categorized
  []
  [:div.transaction-area
   [:section.transaction-list
    [:div.transaction-list-main
     [:ul (let [visibles @(subscribe [::subs/categorized])]
            (doall (for [v visibles] (transaction-card v))))]]
    ]])

(defn main-view
  []
  (let [main-view @(subscribe [::subs/main-view])]    
    (cond
      (= main-view :uncategorized) [uncategorized]
      (= main-view :categorized) [categorized])))

(defn views []
  [:div.with-sidebar
   [:nav.box.sidenav
    [:ul.nav-menu
    ;;  TODO: These links should either point to an actual URL or be buttons, either way, no "javascript:void(0)" should be necessay.
     [:li [:a.menu-link {:href "javascript:void(0)" :on-click (fn [] (dispatch [::events/switch-view :uncategorized]))} [:p.menu-label [:span.icon.fas.fa-clipboard-list] "Uncategorized"] [:span.badge-container [:span.badge @(subscribe [::subs/n-uncategorized])]]]]
     [:li [:a.menu-link {:href "javascript:void(0)" :on-click (fn [] (dispatch [::events/switch-view :categorized]))} [:p.menu-label [:span.icon.fas.fa-clipboard-check] "Categorized"] [:span.badge-container [:span.badge @(subscribe [::subs/n-categorized])]]]]]]
   [:main.main [main-view]]])


(defn
  
  main []

  (dispatch-sync [::events/initialize])

  (rdom/render [views] (js/document.getElementById "app")))

(main)