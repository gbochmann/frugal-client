(ns client.subs
  (:require [re-frame.core :as rf]
            [client.logging :refer [log]]))

(rf/reg-sub ::all-transactions (fn [db _] (log db) (:transactions db)))