(ns zanmi.client
  (:require [clj-http.client :as http]
            [buddy.core.keys :as keys]
            [buddy.sign.jwt :as jwt]
            [cognitect.transit :as transit]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; request utilities                                                        ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- with-transit [& [{:as opts}]]
  (merge {:content-type :transit+json, :accept :transit+json, :as :transit+json}
         opts))

(defn- with-auth
  ([username password] {:basic-auth [username password]})
  ([opts username password] (merge opts (with-auth username password))))

(defn- profile-collection-url [zanmi-url]
  (str zanmi-url "/profiles"))

(defn- profile-url [zanmi-url username]
  (str (profile-collection-url zanmi-url) "/" username))

(defn- auth-url [zanmi-url username]
  (str (profile-url zanmi-url username) "/auth"))

(defn- reset-url [zanmi-url username]
  (str (profile-url zanmi-url username) "/reset"))

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
  (reset-password! [client username reset-token new-password]
    "Reset the password of an existing profile")
  (unregister! [client username password]
    "Remove an existing profile"))

(defrecord UserClient [base-url]
  User
  (register! [_ username password]
    (let [attrs {:username username, :password password}]
      (-> base-url
          (profile-collection-url)
          (http/post (with-transit {:form-params {:profile attrs}}))
          (get-in [:body :auth-token]))))

  (authenticate [_ username password]
    (let [opts (-> (with-transit)
                   (with-auth username password))]
      (-> base-url
          (auth-url username)
          (http/post opts)
          (get-in [:body :auth-token]))))

  (update-password! [_ username password new-password]
    (let [attr {:password new-password}
          opts (-> {:form-params {:profile attr}}
                   (with-transit)
                   (with-auth username password))]
      (-> base-url
          (profile-url username)
          (http/put opts)
          (get-in [:body :auth-token]))))

  (reset-password! [_ username reset-token new-password]
    (let [attr {:password new-password}
          opts (-> {:form-params {:reset-token reset-token
                                  :profile attr}}
                   (with-transit))]
      (-> base-url
          (profile-url username)
          (http/put opts)
          (get-in [:body :auth-token]))))

  (unregister! [_ username password]
    (let [opts (-> (with-transit)
                   (with-auth username password))]
      (-> base-url
          (profile-url username)
          (http/delete opts)
          (get-in [:body :message])))))

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
    (jwt/unsign token verification-key {:alg algorithm}))

  (get-reset-token [_ username]
    (let [signed (jwt/sign {:username username} api-key {:alg :hs512})]
      (-> (reset-url url username)
          (http/post (with-transit {:form-params {:app-token signed}}))
          (get-in [:body :reset-token])))))

(defn application-client [{:keys [algorithm api-key public-key-path url]
                           :as config}]
  (let [pubkey (keys/public-key (:public-key-path config))]
    (-> config
        (dissoc :public-key-path)
        (assoc :verification-key pubkey)
        (map->ApplicationClient))))
