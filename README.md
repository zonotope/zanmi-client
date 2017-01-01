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
(zanmi/update-password! user-client "gwcarver" "pulverized peanuts" "succulent sweet potatos")

;; delete the user's profile from the server
(zanmi/unregister! user-client "gwcarver" "pulverized peanuts")
```

If any of the credentials are incorrect or validations fail, the functions will
thow an exception with the http response code sent by the zanmi server.

#### Application Client

## License

Copyright © 2016 ben lamothe

Distributed under the MIT License
