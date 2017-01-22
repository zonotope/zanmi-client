(ns zanmi.client.application
  (:require [zanmi.client.util.request :refer [request with-token-auth]]
            [zanmi.client.util.url :as url]
            [buddy.core.keys :as keys]
            [buddy.sign.jwt :as jwt]
            [clj-http.client :as http]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; request utils                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- with-app-auth [req token]
  (with-token-auth req token "ZanmiAppToken"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; client protocol                                                          ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Application
  "Manage user profiles"
  (read-token [client token]
    "Unsign the token and verify the signature")
  (get-reset-token [client username]
    "Get a reset token for an existing profile"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; application client                                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord ApplicationClient [algorithm api-key base-url public-key]
  Application
  (read-token [_ token]
    (jwt/unsign token public-key {:alg algorithm}))

  (get-reset-token [_ username]
    (let [app-token (jwt/sign {:username username} api-key {:alg :hs512})]
      (-> (url/reset base-url username)
          (http/post (-> (request)
                         (with-app-auth app-token)))
          (get-in [:body :reset-token])))))

(defn application-client [{:keys [algorithm api-key public-key-path url]
                           :as config}]
  (let [pubkey (keys/public-key (:public-key-path config))]
    (-> config
        (dissoc :public-key-path :url)
        (assoc :public-key pubkey, :base-url url)
        (map->ApplicationClient))))
