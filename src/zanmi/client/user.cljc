(ns zanmi.client.user
  (:require [zanmi.client.request :refer [delete parse-response post put]]
            [zanmi.client.url :as url]))

(defn register [host-url username password]
  (let [attrs {:username username, :password password}]
    (-> {:params {:profile attrs}}
        (post (url/profile-collection host-url))
        (parse-response :auth-token))))

(defn authenticate [host-url username password]
  (-> {:basic-auth [username password]}
      (post (url/auth host-url username))
      (parse-response :auth-token)))

(defn update-password [host-url username password new-password]
  (-> {:basic-auth [username password]
       :params {:profile {:password new-password}}}
      (put (url/profile host-url username))
      (parse-response :auth-token)))

(defn reset-password [host-url username reset-token new-password]
  (-> {:reset-auth reset-token
       :params {:profile {:password new-password}}}
      (put (url/profile host-url username))
      (parse-response :auth-token)))

(defn unregister [host-url username password]
  (-> {:basic-auth [username password]}
      (delete (url/profile host-url username))
      (parse-response :message)))
