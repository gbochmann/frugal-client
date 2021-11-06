(ns client.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [re-frame.core :refer [dispatch-sync]]))

(defn 
  ^:export 

  ;; This triggers a page reload once the code has been reloaded. See https://code.thheller.com/blog/shadow-cljs/2019/08/25/hot-reload-in-clojurescript.html
  ^:dev/after-load 
  
  main []

  (dispatch-sync [:initialise-db])

  (rdom/render [:p "Hello, folks!"] (js/document.getElementById "app")))