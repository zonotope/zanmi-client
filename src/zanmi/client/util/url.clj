(ns zanmi.client.util.url)

(defn profile-collection [base]
  (str base "/profiles"))

(defn profile [base username]
  (str (profile-collection base) "/" username))

(defn auth [base username]
  (str (profile base username) "/auth"))

(defn reset [base username]
  (str (profile base username) "/reset"))
