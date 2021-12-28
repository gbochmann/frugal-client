(ns client.csv (:require [clojure.string :as string]
                         [client.logging :refer [log]]
                         [cljs-time.format :refer [formatter parse]]))

(defmulti heading identity)

(defmethod heading "Auftraggeber/Empfänger" [_] "initiator-recipient")

(defmethod heading :default [h] h)

(defn format-headings [headings] (map heading headings))

(defn csvstr->map
  ([] [])
  ([csvstr]
   (as-> csvstr s
     (string/split-lines s)
     (map #(string/split % #";") s)
     (map #(zipmap (-> s first format-headings) %) (rest s))
     (into [] s))))

(defn naive-str->int
  "Removes all dots and commas from a string and then attempts to parse it as an integer."
  [s] (-> s (string/replace #"[\.,]" "") js/parseInt))

(defn income
  "Returns 0 if an int is negative, returns the int if it's positive."
  [value] (if (neg? value) 0 value))

(defn expense
  "Returns 0 if an int is positive, returns the int if it's postive."
  [value] (if (pos? value) 0 (Math/abs value)))

(def little-endian-dotted (formatter "dd.MM.yyyy"))

(defn date->vec [s] (-> s (string/split #"\.") (->> (map js/parseInt)) vec))

(defn ing->transaction
  ([] [])
  ([{:strs [Buchung Verwendungszweck initiator-recipient Betrag Buchungstext] :as data}]
   (when (not (map? data)) (log "Not a map in ing->transaction."))
   {:date (date->vec Buchung)
    :note (str Buchungstext " " initiator-recipient " " Verwendungszweck)
    :expense (-> Betrag naive-str->int expense)
    :income (-> Betrag naive-str->int income)}))

(defn fidor->transaction 
  ([] {})
  ([{:strs [Datum Beschreibung Beschreibung2 Wert]}]
   {:date (date->vec Datum)
    :note (string/trim (str Beschreibung " " Beschreibung2))
    :expense (-> Wert naive-str->int expense)
    :income (-> Wert naive-str->int income)}))