(ns client.db
  (:require [reagent.core :as reagent]))

(def
  
  init-db 

  (reagent/atom {:transactions {}}))