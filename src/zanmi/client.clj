(ns zanmi.client
  (:require [clj-http.client :as http]
            [buddy.core.keys :as keys]
            [buddy.sign.jwt :as jwt]
            [cognitect.transit :as transit]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; request utilities                                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- with-transit [opts]
  (merge {:content-type :transit+json, :accept :transit+json, :as :transit+json}
         opts))

(defn- profile-collection-url [zanmi-url]
  (str zanmi-url "/profiles"))

(defn- profile-url [zanmi-url username]
  (str (profile-collection-url zanmi-url) "/" username))

(defn- auth-url [zanmi-url username]
  (str (profile-url zanmi-url username) "/auth"))

(defn- reset-url [zanmi-url username]
  (str (profile-url zanmi-url username) "/reset"))

(defn- alg-key [algorithm]
  (let [keymap {:ecdsa256 :es256, :ecdsa512 :es512, :rsa-pss256 :ps256,
                :rsa-pss512 :ps512, :sha512 :hs512}]
    (get keymap algorithm)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; user client                                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol User
  "Authenticate users"
  (register! [client username password]
    "Add a new profile")
  (authenticate [client username password]
    "Validate password against an existing profile")
  (update-password! [client username password new-password]
    "Update the password of an existing profile")
  (unregister! [client username password]
    "Remove an existing profile"))

(defrecord UserClient [url]
  User
  (register! [_ username password]
    (-> (profile-collection-url url)
        (http/post (with-transit {:form-params {:username username
                                                :password password}}))
        (get-in [:body :auth-token])))

  (authenticate [_ username password]
    (-> (auth-url url username)
        (http/post (with-transit {:form-params {:password password}}))
        (get-in [:body :auth-token])))

  (update-password! [_ username password new-password]
    (-> (profile-url url username)
        (http/put (with-transit {:form-params {:password password
                                               :new-password new-password}}))
        (get-in [:body :auth-token])))

  (unregister! [_ username password]
    (-> (profile-url url username)
        (http/delete (with-transit {:form-params {:password password}}))
        (get-in [:body :message]))))

(defn user-client [{url :url :as config}]
  (->UserClient url))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; application client                                                       ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Application
  "Manage user profiles"
  (read-token [client token]
    "Unsign the token and verify the signature")
  (get-reset-token [client username]
    "Get a reset token for an existing profile"))

(defrecord ApplicationClient [algorithm api-key url verification-key]
  Application
  (read-token [_ token]
    (jwt/unsign token verification-key {:alg (alg-key algorithm)}))

  (get-reset-token [_ username]
    (let [signed (jwt/sign {:username username} api-key {:alg :hs512})]
      (-> (reset-url url username)
          (http/post (with-transit {:form-params {:request-token signed}}))
          (get-in [:body :reset-token])))))

(defn application-client [{:keys [algorithm api-key public-key-path url]
                           :as config}]
  (let [pubkey (keys/public-key (:public-key-path config))]
    (-> config
        (dissoc :public-key-path)
        (assoc :verification-key pubkey)
        (map->ApplicationClient))))
