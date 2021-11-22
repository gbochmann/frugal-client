(ns client.fx
  (:require [re-frame.core :as rf]
            [client.logging :refer [log]]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

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

(rf/reg-fx
 
 ::export-category-csv
 
 (fn [csv] 
   (go (let [options {:types [{:description "Text Files" :accept {"text/plain" [".csv"]}}]}
             handle (<p! (.showSaveFilePicker js/window options))
             writable (<p! (.createWritable handle))]
         (<p! (.write writable csv))
         (<p! (.close writable))))))