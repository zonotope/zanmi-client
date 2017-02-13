(ns zanmi.client.cljs-request
  (:require [zanmi.client.token-auth :refer [wrap-token-auth wrap-reset-auth]]
            [cljs.core.async :refer [chan pipe]]))

(defn- wrap-basic-auth [{:keys [basic-auth] :as req}]
  (if basic-auth
    (assoc req :basic-auth (zipmap [:username :password] basic-auth))
    req))

(defn- unwrap-credentials [req]
  (assoc req :with-credentials? false))

(defn wrap-auth [req]
  (-> req
      (wrap-basic-auth)
      (unwrap-credentials)))

(defn wrap-params [{:keys [params] :as req}]
  (if params
    (-> req
        (dissoc :params)
        (assoc :transit-params params))
    req))

(defn parse-response [resp-chan node]
  (let [parse-fn    (fn [resp] (get-in resp [:body node]))
        parsed-chan (chan 1 (map parse-fn))]
    (pipe resp-chan parsed-chan)))
