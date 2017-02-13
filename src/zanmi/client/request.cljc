(ns zanmi.client.request
  (:require [#?(:clj  zanmi.client.clj-request
                :cljs zanmi.client.cljs-request) :as platform]
            [#?(:clj  clj-http.client
                :cljs cljs-http.client) :as http]))

(defn- request [opts]
  (-> opts
      (platform/wrap-auth)
      (platform/wrap-params)))

(defn delete [opts url]
  (->> (request opts)
       (http/delete url)))

(defn post [opts url]
  (->> (request opts)
       (http/post url)))

(defn put [opts url]
  (->> (request opts)
       (http/put url)))

(defn parse-response [resp node]
  (platform/parse-response resp node))
