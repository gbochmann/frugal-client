(ns client.fx
  (:require [re-frame.core :as rf]
            [client.logging :refer [log]]))

(rf/reg-fx
 
 ::export-csv
 
 (fn [csv] (let
         [options {:types [{:description "Text Files" :accept {"text/plain" [".csv"]}}]}]
          (.then 
           (.showSaveFilePicker js/window options)
           (fn [handle] 
             (.then 
              (.createWritable handle) 
              (fn [writable] 
                (.then 
                 (.write writable csv)
                 (fn [] (.close writable))))))))))