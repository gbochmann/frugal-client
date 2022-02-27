(ns client.events-test
  (:require [cljs.test :refer-macros [deftest is]]
            [client.events :as events]))

(defn log [s]
  (js/console.log (clj->js s))
  s)

(def category-sums {["04" "2020"] {"Einkaufen" 896} ["04" "2021"] {"Einkaufen" 1198} ["12" "2021"] {"Restaurants" 3324 "Einkaufen" 500}})

(def short-string "Buchung;Valuta;Auftraggeber/Empfänger;Buchungstext;Kategorie;Verwendungszweck;Saldo;Währung;Betrag;Währung\n07.04.2021;07.04.2021;VISA EDEKA AKTIV-MARKT;Lastschrift;Lebensmittel und Haushalt;NR XXXX 7033 JENA KAUFUMSATZ 03.04 195518 ARN74627641095000141960901 Apple Pay;687,44;EUR;-6,98;EUR\n07.04.2021;07.04.2021;Salesforce Germany GmbH;Lastschrift;Gehalt;Gehalt April 2020;6100;EUR;5325,00;EUR\n")

(deftest test-transactions->category-freq
  (is (=
       (events/transactions->category-sum
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
       (events/category-sum->csv category-sums)
       "Year;Month;Einkaufen;Restaurants\n2020;April;896;0\n2021;April;1198;0\n2021;December;500;3324\n")))

(defn event-result [s] (clj->js {:target {:result s}}))

(defn make-uuid-mock []
  (let [counter (atom 1)]
    (fn [] (str "UUID-" (swap! counter inc)))))

(deftest test-add-fidor-transactions
  (is (=
       (with-redefs [random-uuid (make-uuid-mock)]
         (events/add-fidor-transactions
          {:transactions {"UUID-0" {:uuid "UUID-0"} "UUID-1" {:uuid "UUID-1"}}}
          [::events/add-fidor-transactions
           (event-result "Datum;Beschreibung;Beschreibung2;Wert\n02.11.2021;Irgendwas ausgegeben;;-5.99\n13.01.2022;Noch was ausgegeben;Für Synths;-689.99\n")]))

       {:transactions {"UUID-0" {:uuid "UUID-0"}
                       "UUID-1" {:uuid "UUID-1"}
                       "UUID-2" {:date [2 11 2021] :note "Irgendwas ausgegeben" :expense 599 :income 0 :selected false :category nil :uuid "UUID-2"}
                       "UUID-3" {:date [13 1 2022] :note "Noch was ausgegeben Für Synths" :expense 68999 :income 0 :selected false :category nil :uuid "UUID-3"}}
        :uncategorized-transactions (list "UUID-2" "UUID-3")})))

(def db-transactions {"UUID-0" {:uuid "UUID-0"}
                      "UUID-1" {:uuid "UUID-1"}
                      "UUID-2" {:date [7 4 2021] :note "SOFORT GEKAUFT SCHOKOLADENHUNGER NAHRUNGSAUFNAHME" :expense 698 :income 0 :selected false :category nil :uuid "UUID-2"}
                      "UUID-3" {:date [13 1 2022] :note "Lastschrift Salesforce Germany GmbH Gehalt April 2020" :expense 0 :income 10000000 :selected false :category nil :uuid "UUID-3"}})

(deftest test-add-ing-transactions
  (is (=
       (with-redefs [random-uuid (make-uuid-mock)]
         (events/add-ing-transactions
          {:transactions {"UUID-0" {:uuid "UUID-0"} "UUID-1" {:uuid "UUID-1"}}}
          [::events/add-ing-transactions
           (event-result "Buchung;Valuta;Auftraggeber/Empfänger;Buchungstext;Kategorie;Verwendungszweck;Saldo;Währung;Betrag;Währung\n07.04.2021;07.04.2021;SCHOKOLADENHUNGER;SOFORT GEKAUFT;SUESSKRAM;NAHRUNGSAUFNAHME;687,44;EUR;-6,98;EUR\n13.01.2022;13.01.2022;Salesforce Germany GmbH;Lastschrift;Gehalt;Gehalt April 2020;6100;EUR;100000,00;EUR\n")]))

       {:transactions db-transactions
        :uncategorized-transactions (list "UUID-2" "UUID-3")})))

(def t3 {:date [13 1 2022] :note "Lastschrift Salesforce Germany GmbH Gehalt April 2020" :expense 0 :income 10000000 :selected false :category nil :uuid "UUID-3"})

(def valid-transactions [{:date [7 4 2021] :note "SOFORT GEKAUFT SCHOKOLADENHUNGER NAHRUNGSAUFNAHME" :expense 698 :income 0 :selected false :category nil :uuid "UUID-2"}
                         t3])

(deftest test-filter-by-note
  (is (= (events/filter-by-note "Salesforce" valid-transactions) (list t3))))


(let [transactions {"UUID-NOPE" {:date [1 1 2022] :note "NotAHit" :uuid "UUID-NOPE"}
                    "UUID-YUP" {:date [2 1 2022] :note "Cognitect" :uuid "UUID-YUP"}
                    "UUID-YUP2" {:date [1 1 2022] :note "Cognitect b" :uuid "UUID-YUP2"}}
      initial-db {:transactions transactions
                  :uncategorized-transactions (list "UUID-YUP" "UUID-NOPE" "UUID-YUP2")
                  :filter-term nil}]
  
  (deftest test-filter-table
    (is (= (events/filter-table initial-db [:events/filter-table "Cognitect"])
           {:transactions transactions
            :uncategorized-transactions (list "UUID-YUP" "UUID-YUP2")
            :filter-term "Cognitect"})))

  (deftest test-filter-table-blank
    (is (= (events/filter-table initial-db [:events/filter-table nil])
           {:transactions transactions
            :uncategorized-transactions (list "UUID-YUP" "UUID-NOPE" "UUID-YUP2")
            :filter-term nil})))
  
  (deftest test-clear-filter
    ;; Checks whether clear-filter event returns the db to (almost) its initial state. 
    (is (=
         (events/clear-filter initial-db [::events/clear-filter])
         initial-db)))
  )

