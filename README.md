# zanmi-client
Clojure [zanmi](https://github.com/zonotope/zanmi) client.

## Usage
Add this dependency to your project:

[![Clojars Project](https://img.shields.io/clojars/v/zanmi-client.svg)](https://clojars.org/zanmi-client)

Then require the client namespace:
```clojure
(ns my.beautiful.namespace
  (:require [zanmi.client :as zanmi]))
```

### Clients
To make requests to a zanmi server, you'll first need a client object. There are
two types of client objects, depending on what type of requests you'll need to
make.

#### User Client
Use a user client to manage a particular user's zanmi profile: create profiles,
get auth tokens, update passwords, and delete profiles.
```clojure
;; get a new user client with the full url of the zanmi server
(def user-client (zanmi/user-client {:url "http://<zanmi server host>:<zanmi port>"}))

;; create a new profile. if all is well, this function will return an auth token
(zanmi/register! user-client "gwcarver" "pulverized peanuts")

;; authenticate the user. if the credentials are correct, this fn will return a
;; new auth token
(zanmi/authenticate user-client "gwcarver" "pulverized peanuts")

;; update the users password. this function returns a new auth token if all is
;; well
(zanmi/update-password! user-client "gwcarver" "pulverized peanuts" "succulent sweet potatoes")

;; reset the user's password with a valid reset token (more on that below) using
;; the `reset-password!` function
(zanmi/reset-password! user-client "gwcarver" "<reset token>" "succulent sweet potatoes")

;; delete the user's profile from the server
(zanmi/unregister! user-client "gwcarver" "succulent sweet potatoes")
```

If any of the credentials are incorrect or validations fail, the functions will
thow an exception with the http response code sent by the zanmi server.

#### Application Client
Use an application client to integrate an application with zanmi. The
application client allows applications to get new password reset tokens for
users as well validate authentication tokens passed into the application from
user requests.

```clojure
;; get a new application client with the zanmi token signing algorithm, api key,
;; public key path, and url

(def app-client
  (let [zanmi-app-client-config {:algorithm :rsa-pss512
                               :api-key "some long string"
                               :public-key-path "/path/to/zanmi-public-key"
                               :url "http://<zanmi host>:<zanmi port>"}]
    (zanmi/application-client zanmi-app-client-config)))

;; use `read-token` to unsign and verify that an auth token is valid and not
;; expired.
(zanmi/read-token app-client "user auth token string")

;; use `get-reset-token` to get a password reset token for if a user has
;; forgotten their password. send this token to the user's email address, and
;; valid reset tokens allow them to reset their passwords without entering the
;; old one.
(zanmi/get-reset-token app-client "gwcarver")
```

## License

Copyright © 2016 ben lamothe

Distributed under the MIT License
