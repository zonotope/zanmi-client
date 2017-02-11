(ns zanmi.client.application
  (:require [zanmi.client.request :refer [parse-response post]]
            [zanmi.client.url :as url]
            [buddy.core.keys :as keys]
            [buddy.sign.jwt :as jwt]))

(defn get-reset-token [{:keys [api-key host-url] :as client} username]
  (let [app-token (jwt/sign {:username username} api-key {:alg :hs512})]
    (-> {:app-auth app-token}
        (post (url/reset host-url username))
        (parse-response :reset-token))))

(defn read-auth-token [{:keys [algorithm public-key] :as client} token]
  (jwt/unsign token public-key {:alg algorithm}))

(defrecord ApplicationClient [algorithm api-key public-key host-url])

(defn application-client [{:keys [algorithm api-key public-key-path host-url]
                           :as config}]
  (let [pubkey (keys/public-key (:public-key-path config))]
    (-> config
        (dissoc :public-key-path)
        (assoc :public-key pubkey)
        (map->ApplicationClient))))
