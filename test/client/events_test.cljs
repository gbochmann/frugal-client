(ns client.events-test
  (:require [cljs.test :refer-macros [deftest is]]
            [client.events :refer [transactions->category-sum category-sum->csv]]))

(defn log [s]
  (js/console.log (clj->js s))
  s)

(def category-sums {["04" "2020"] {"Einkaufen" 896} ["04" "2021"] {"Einkaufen" 1198} ["12" "2021"] {"Restaurants" 3324 "Einkaufen" 500}})

(deftest test-transactions->category-freq
  (is (=
       (transactions->category-sum
        {:uuid1 {:date "07.04.2020"
                 :note "VISA EDEKA AKTIV-MARKT NR XXXX 7033 JENA KAUFUMSATZ 03.04 195518 ARN74627641095000141960901 Apple Pay"
                 :expense 896
                 :income 0
                 :category "Einkaufen"}
         :uuid2 {:date "07.04.2021"
                 :note "VISA EDEKA AKTIV-MARKT NR XXXX 7033 JENA KAUFUMSATZ 03.04 195518 ARN74627641095000141960901 Apple Pay"
                 :expense 698
                 :income 0
                 :category "Einkaufen"}
         :uuid3 {:date "08.04.2021"
                 :note "VISA EDEKA AKTIV-MARKT NR XXXX 7033 JENA KAUFUMSATZ 03.04 195518 ARN74627641095000141960901 Apple Pay"
                 :expense 500
                 :income 0
                 :category "Einkaufen"}
         :uuid4 {:date "08.04.2021"
                 :note "Salesforce"
                 :expense 0
                 :income 230000
                 :category nil}
         :uuid5 {:date "07.12.2021"
                 :note "Cafe Lenz"
                 :expense 3324
                 :income 0
                 :category "Restaurants"}
         :uuidd6 {:date "08.12.2021"
                  :note "VISA EDEKA AKTIV-MARKT NR XXXX 7033 JENA KAUFUMSATZ 03.04 195518 ARN74627641095000141960901 Apple Pay"
                  :expense 500
                  :income 0
                  :category "Einkaufen"}})

          category-sums)))

(deftest test-category-sum->csv
  (is (=
       (category-sum->csv category-sums)
       "Year;Month;Einkaufen;Restaurants\n2020;April;896;0\n2021;April;1198;0\n2021;December;500;3324\n")))