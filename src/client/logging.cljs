(ns client.logging)

(defn log [s] 
  (js/console.log (clj->js s)) 
  s)