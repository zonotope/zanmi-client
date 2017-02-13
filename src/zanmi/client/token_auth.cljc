(ns zanmi.client.token-auth)

(defn wrap-token-auth [req token scheme]
  (let [header (str scheme " " token)]
    (assoc-in req [:headers "authorization"] header)))

(defn wrap-reset-auth [{:keys [reset-auth] :as req}]
  (if reset-auth
    (-> req
        (dissoc :reset-auth)
        (wrap-token-auth reset-auth "ZanmiResetToken"))
    req))
