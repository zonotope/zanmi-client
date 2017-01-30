(ns zanmi.client.request
  #?(:cljs (:require [cljs.core.async :refer [chan pipe]]
                     [cljs-http.client :as http])
     :clj (:require [clj-http.client :as http])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; auth                                                                     ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-token-auth [req token scheme]
  (let [header (str scheme " " token)]
    (assoc-in req [:headers "authorization"] header)))

#?(:clj (defn- wrap-app-auth [{:keys [app-auth] :as req}]
          (if app-auth
            (-> req
                (dissoc :app-auth)
                (wrap-token-auth app-auth "ZanmiAppToken"))
            req))

   :cljs (defn wrap-basic-auth [{:keys [basic-auth] :as req}]
           (if basic-auth
             (assoc req :basic-auth (zipmap [:username :password] basic-auth))
             req)))

(defn- wrap-reset-auth [{:keys [reset-auth] :as req}]
  (if reset-auth
    (-> req
        (dissoc :reset-auth)
        (wrap-token-auth reset-auth "ZanmiResetToken"))
    req))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; params                                                                   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- wrap-params [{:keys [params] :as opts}]
  (if params
    (-> opts
        (dissoc :params)
        #?(:clj  (assoc :form-params params)
           :cljs (assoc :transit-params params)))
    opts))

(defn- wrap-transit-params [opts]
  (let [with-params (wrap-params opts)]
    #?(:clj (assoc with-params
                   :accept       :transit+json
                   :as           :transit+json
                   :content-type :transit+json)
       :cljs with-params)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; request/response                                                         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn request [{:keys [params] :as opts}]
  (-> opts
      #?(:clj  (wrap-app-auth)
         :cljs (wrap-basic-auth))
      (wrap-reset-auth)
      (wrap-transit-params)))

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
  #?(:clj  (get-in resp [:body node])

     :cljs (let [parse-fn    (fn [resp] (get-in resp [:body node]))
                 parsed-chan (chan 1 (map parse-fn))]
             (pipe resp parsed-chan))))
