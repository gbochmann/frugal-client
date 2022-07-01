(ns client.db
  (:require [reagent.core :as reagent]))

(def

  init-db
  
  {:transactions {}
   :selected-transactions []
   :single-assignment true
   :main-view :uncategorized
   :categorized-transactions []})