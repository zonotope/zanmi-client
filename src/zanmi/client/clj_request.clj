(ns zanmi.client.clj-request
  (:require [zanmi.client.token-auth :refer [wrap-token-auth wrap-reset-auth]]))

(defn- wrap-app-auth [{:keys [app-auth] :as req}]
  (if app-auth
    (-> req
        (dissoc :app-auth)
        (wrap-token-auth app-auth "ZanmiAppToken"))
    req))

(defn wrap-auth [req]
  (-> req
      (wrap-app-auth)
      (wrap-reset-auth)))

(defn wrap-params [{:keys [params] :as req}]
  (-> req
      (assoc :accept       :transit+json
             :as           :transit+json
             :content-type :transit+json)
      (as-> transit-req (if params
                          (-> (dissoc transit-req :params)
                              (assoc :form-params params))
                          transit-req))))

(defn parse-response [resp node]
  (get-in resp [:body node]))
