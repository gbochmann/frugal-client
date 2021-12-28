(ns client.csv-test
  (:require [cljs.test :refer-macros [deftest is]]
            [client.csv :refer [fidor->transaction]]))



(deftest happy-fidor-transaction
  (is (=
       (fidor->transaction {"Datum" "20.11.2021"
                            "Beschreibung" "MasterCard Fremdwaehrungsgebuehr in Hoehe von 0,07 Euro"
                            "Beschreibung2" ""
                            "Wert" "-0.07"})
       {:date [20 11 2021]
        :note "MasterCard Fremdwaehrungsgebuehr in Hoehe von 0,07 Euro"
        :expense 7
        :income 0})))

(deftest empty-fidor-transaction
  (is (=
       (fidor->transaction)
       {})))