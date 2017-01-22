(ns zanmi.client.user
  (:require [zanmi.client.util.request :refer [request with-token-auth]]
            [zanmi.client.util.url :as url]
            [clj-http.client :as http]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; request utils                                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- with-basic-auth [req username password]
  (assoc req :basic-auth [username password]))

(defn- with-reset-auth [req token]
  (with-token-auth req token "ZanmiResetToken"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; client protocol                                                          ;;
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; user client                                                              ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord UserClient [base-url]
  User
  (register! [_ username password]
    (let [attrs {:username username, :password password}]
      (-> base-url
          (url/profile-collection)
          (http/post (request :form-params {:profile attrs}))
          (get-in [:body :auth-token]))))

  (authenticate [_ username password]
    (let [req (-> (request)
                  (with-basic-auth username password))]
      (-> base-url
          (url/auth username)
          (http/post req)
          (get-in [:body :auth-token]))))

  (update-password! [_ username password new-password]
    (let [attr {:password new-password}
          req (-> (request :form-params {:profile attr})
                  (with-basic-auth username password))]
      (-> base-url
          (url/profile username)
          (http/put req)
          (get-in [:body :auth-token]))))

  (reset-password! [_ username reset-token new-password]
    (let [attr {:password new-password}
          req (-> (request :form-params {:profile attr})
                  (with-reset-auth reset-token))]
      (-> base-url
          (url/profile username)
          (http/put req)
          (get-in [:body :auth-token]))))

  (unregister! [_ username password]
    (let [req (-> (request)
                  (with-basic-auth username password))]
      (-> base-url
          (url/profile username)
          (http/delete req)
          (get-in [:body :message])))))

(defn user-client [{url :url :as config}]
  (->UserClient url))
