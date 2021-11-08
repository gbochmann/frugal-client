(ns client.csv (:require [clojure.string :as cljstr]
                         [client.logging :refer [log]]
                         [clojure.spec.alpha :as spec]))

;; #?(:clj (defn open-csv
;;           ([] "")
;;           ([file-path] (-> file-path slurp))))

;; #?(:clj (def parse-int #(Integer/parseInt %))
;;    :cljs (def parse-int js/parseInt))

(defmulti heading identity)

(defmethod heading "Auftraggeber/Empfänger" [_] "initiator-recipient")

(defmethod heading :default [h] h)

(defn format-headings [headings] (map heading headings))

(defn csvstr->map
  ([] [])
  ([csvstr]
   (as-> csvstr s
     (cljstr/split-lines s)
     (map #(cljstr/split % #";") s)
     (map #(zipmap (-> s first format-headings) %) (rest s))
     (into [] s))))

(defn naive-str->int
  "Removes all dots and commas from a string and then attempts to parse it as an integer."
  [s] (-> s (cljstr/replace #"[\.,]" "") js/parseInt))

(defn income
  "Returns 0 if an int is negative, returns the int if it's positive."
  [value] (if (neg? value) 0 value))

(defn expense
  "Returns 0 if an int is positive, returns the int if it's postive."
  [value] (if (pos? value) 0 (Math/abs value)))

;; #?(:cljs (defn log [s] (js/console.log (clj->js s)))\
;;          :clj (defn log [x] x))

(defn ing->transaction
  ([] [])
  ([{:strs [Buchung Verwendungszweck initiator-recipient Betrag] :as data}]
   (when (not (map? data)) (log "Not a map in ing->transaction."))
   {:date Buchung
    :note (str initiator-recipient " " Verwendungszweck)
    :expense (-> Betrag naive-str->int expense)
    :income (-> Betrag naive-str->int income)}))