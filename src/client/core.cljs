(ns client.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :refer [dispatch-sync dispatch]]
            [client.csv :refer [csvstr->map ing->transaction]]
            [client.logging :refer [log]]))


(defn read-file [onload-callback event]
  (let [file (-> event .-target .-files (aget 0))
        r (js/FileReader.)]

    (set! r -onload onload-callback)

    (.readAsText r file)))

(defn dispatch-transaction [t] (dispatch [:events/add-transaction t]) t)

(defn add-uuid [t] (assoc t :uuid (random-uuid)))

(def transformations (comp add-uuid ing->transaction))

(defn transform-csv [event] (-> event 
                                .-target 
                                .-result 
                                csvstr->map 
                                (->> (map transformations) (map dispatch-transaction) doall)))

(defn file-input []
  [:div.container
   [:div.box
    [:div.block [:label {:for "csv-input"} "Add CSV file"]]
    [:div.block
     [:input#csv-input {:type "file"
                        :name "csv-input"
                        :accept "text/csv"
                        :on-change (partial read-file transform-csv)}]]]])

(defn
  ^:export

  ;; This triggers a page reload once the code has been reloaded. See https://code.thheller.com/blog/shadow-cljs/2019/08/25/hot-reload-in-clojurescript.html
  ^:dev/after-load

  main []

  (dispatch-sync [:initialize])

  (rdom/render [file-input] (js/document.getElementById "app")))