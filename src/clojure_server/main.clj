(ns clojure-server.main
  (:require [clojure-server.ttt-route :as ttt]
            [tic-tac-toe.data.data-io :as data-io])
  (:import
           (com.cleanCoders ArgParser Printables Router Server ServerSocket)
           (com.cleanCoders.routes FormRouteHandler
                                   GuessRouteHandler
                                   HelloRouteHandler
                                   ListingRouteHandler
                                   PingRouteHandler)))

(reset! data-io/data-store :psql)

(defn add-route [router path route-handler]
  (.addRoute router path route-handler))

(defn create-RouteHandler [route-handler]
  route-handler)

(defn add-routes [router arg-parser]
  (add-route router "/hello" (create-RouteHandler (HelloRouteHandler.)))
  (add-route router "/ping" (create-RouteHandler (PingRouteHandler.)))
  (add-route router "/listing" (create-RouteHandler (ListingRouteHandler. (.getRoot arg-parser))))
  (add-route router "/form" (create-RouteHandler (FormRouteHandler.)))
  (add-route router "/guess" (create-RouteHandler (GuessRouteHandler.)))
  (add-route router "/ttt" (create-RouteHandler (ttt/->TttRouteHandler))))

(defn print-startup-config [arg-parser]
  (Printables/printStartupConfig (.getRoot arg-parser) (.getPort arg-parser)))

(defn run-server [router arg-parser]
  (let [java-socket (java.net.ServerSocket. (.getPort arg-parser))]
    (print-startup-config arg-parser)
    (.run (Server. router (ServerSocket. java-socket)))))

(defn runnable? [arg-parser]
  (.getRunStatus arg-parser))

(defn -main [& args]
  (let [router (Router.) arg-parser (ArgParser.)]
    (.parseArgs arg-parser (into-array String args))
    (add-routes router arg-parser)
    (if (runnable? arg-parser)
      (run-server router arg-parser) nil)))