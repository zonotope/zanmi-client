(ns zanmi.client.util.request
  (:require [buddy.sign.jwt :as jwt]
            [cognitect.transit :as transit]))

(defn- with-transit [& [{:as req}]]
  (assoc req
         :accept       :transit+json
         :as           :transit+json
         :content-type :transit+json))

(defn with-token-auth [req token scheme]
  (let [header (str scheme " " token)]
    (assoc-in req [:headers "authorization"] header)))

(defn request [& opts]
  (with-transit opts))
