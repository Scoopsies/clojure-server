(ns clojure-server.main-spec
  (:require [speclj.core :refer :all]
            [clojure-server.main :as sut]
            [tic-tac-toe.data.data-io :as data-io])
  (:import (com.cleanCoders ArgParser Router)
           (com.cleanCoders.routes FormRouteHandler
                                   GuessRouteHandler
                                   HelloRouteHandler
                                   ListingRouteHandler
                                   PingRouteHandler)))

(defmacro should-add-route []
  `())

(describe "main"
  (with-stubs)

  (context "data-io"
    (it "sets the data storage method as :edn"
      (should= :psql @data-io/data-store))
    )

  (context "add-routes"
    (it "adds the hello route handler to /hello"
      (let [router (Router.)
            arg-parser (ArgParser.)
            hello-Handler (HelloRouteHandler.)]
        (with-redefs [sut/add-route (stub :add-route)
                      sut/create-RouteHandler (stub :add-route {:return hello-Handler})]
          (sut/add-routes router arg-parser)
          (should-have-invoked :add-route {:with [router "/hello" hello-Handler]}))))

    (it "adds the ping route handler to /ping"
      (let [router (Router.)
            arg-parser (ArgParser.)
            ping-Handler (PingRouteHandler.)]
        (with-redefs [sut/add-route (stub :add-route)
                      sut/create-RouteHandler (stub :add-route {:return ping-Handler})]
          (sut/add-routes router arg-parser)
          (should-have-invoked :add-route {:with [router "/ping" ping-Handler]}))))

    (it "adds the form route to /form"
      (let [router (Router.)
            arg-parser (ArgParser.)
            form-handler (FormRouteHandler.)]
        (with-redefs [sut/add-route (stub :add-route)
                      sut/create-RouteHandler (stub :add-route {:return form-handler})]
          (sut/add-routes router arg-parser)
          (should-have-invoked :add-route {:with [router "/form" form-handler]}))))

    (it "adds the listing route handler to /listing"
      (let [router (Router.)
            arg-parser (ArgParser.)
            listing-Handler (ListingRouteHandler. ".")]
        (with-redefs [sut/add-route (stub :add-route)
                      sut/create-RouteHandler (stub :add-route {:return listing-Handler})]
          (sut/add-routes router arg-parser)
          (should-have-invoked :add-route {:with [router "/listing" listing-Handler]}))))

    (it "adds the guess route handler to /guess"
      (let [router (Router.)
            arg-parser (ArgParser.)
            guess-handler (GuessRouteHandler.)]
        (with-redefs [sut/add-route (stub :add-route)
                      sut/create-RouteHandler (stub :add-route {:return guess-handler})]
          (sut/add-routes router arg-parser)
          (should-have-invoked :add-route {:with [router "/guess" guess-handler]}))))
    )
  )