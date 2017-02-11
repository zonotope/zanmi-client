(ns zanmi.client.user
  (:require [zanmi.client.request :refer [delete parse-response post put]]
            [zanmi.client.url :as url]))

(defn register [{:keys [host-url] :as client} username password]
  (let [attrs {:username username, :password password}]
    (-> {:params {:profile attrs}}
        (post (url/profile-collection host-url))
        (parse-response :auth-token))))

(defn authenticate [{:keys [host-url] :as client} username password]
  (-> {:basic-auth [username password]}
      (post (url/auth host-url username))
      (parse-response :auth-token)))

(defn update-password [{:keys [host-url] :as client} username password
                       new-password]
  (-> {:basic-auth [username password]
       :params {:profile {:password new-password}}}
      (put (url/profile host-url username))
      (parse-response :auth-token)))

(defn reset-password [{:keys [host-url] :as client} username reset-token
                      new-password]
  (-> {:reset-auth reset-token
       :params {:profile {:password new-password}}}
      (put (url/profile host-url username))
      (parse-response :auth-token)))

(defn unregister [{:keys [host-url] :as client} username password]
  (-> {:basic-auth [username password]}
      (delete (url/profile host-url username))
      (parse-response :message)))

(defrecord UserClient [host-url])

(defn user-client [host-url]
  (->UserClient host-url))
