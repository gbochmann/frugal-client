(ns client.db
  (:require [reagent.core :as reagent]))

(def

  init-db
  
  {:transactions {}
   :selected-transactions []
   :all-selected false})