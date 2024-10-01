(ns clojure-server.ttt-route-spec
  (:require [clojure.string :as str]
            [speclj.core :refer :all]
            [clojure-server.ttt-route :as sut]
            [tic-tac-toe.state-initializer :as initializer]
            [tic-tac-toe.printables :as printables])
  (:import (com.cleanCoders HttpRequest)
           (java.io ByteArrayInputStream)))

(def ttt-route-handler (sut/->TttRouteHandler))

(defn build-request [state selection]
  (HttpRequest.
    (ByteArrayInputStream.
      (.getBytes
        (str
          "POST /TTT HTTP/1.1\r\n"
          "Cookie: state=" state "; Path=/; HttpOnly\r\n"
          "Content-Type: application/x-www-form-urlencoded\r\n"
          "Content-Length: 11\r\n"
          "\r\n"
          "selection=" selection "\r\n")))))

(describe "ttt-route"

  (context "parse-cookie"
    (it "returns a single cookie request"
      (should= {:state {}} (sut/parse-cookie "state={}; Path=/; HttpOnly")))

    (it "returns a multiple cookie request"
      (should= {:state {}
                :answer 5
                :guess 15} (sut/parse-cookie "state={}; answer=5; guess=15; Path=/; HttpOnly")))
    )

  (context "get-old-state"
    (it "returns the old state stored in the cookie of the request"
      (should= {} (sut/get-old-state (build-request {} 1)))
      (should= {:key :value} (sut/get-old-state (build-request {:key :value} 1))))

    (it "returns the old state if there are multiple cookies"
      (should= {} (sut/get-old-state
                    (HttpRequest.
                      (ByteArrayInputStream.
                        (.getBytes
                          (str
                            "POST /TTT HTTP/1.1\r\n"
                            "Cookie: answer=45; guess=3; state={}; Path=/; HttpOnly\r\n"
                            "Content-Type: application/x-www-form-urlencoded\r\n"
                            "Content-Length: 11\r\n"
                            "\r\n"
                            "selection=1\r\n")))))))
    )

  (context "get-state"
    (context "menu"
      (it "returns new game state."
        (should= {:printables printables/player-x-printables} (sut/get-state {:last-game {}} "2")))

      (it "returns player o menu"
        (should= {:printables printables/player-o-printables "X" :human} (sut/get-state {} "1")))
      )

    (context "human v human"
      (let [old-state {:ui :tui, "X" :human, "O" :human, :board-size :3x3, :board [0 1 2 3 4 5 6 7 8]}]
        (it "returns the correct updated board in a human v human game for selection 1"
          (should= ["X" 1 2 3 4 5 6 7 8] (:board (sut/get-state old-state "1"))))

        (it "returns the correct updated board in a human v human game for selection 2"
          (should= [0 "X" 2 3 4 5 6 7 8] (:board (sut/get-state old-state "2"))))

        (it "returns the correct updated board in a human v human game for selection 3"
          (should= [0 1 "X" 3 4 5 6 7 8] (:board (sut/get-state old-state "3"))))))

    (context "human v ai"
      (let [old-state {:ui :tui, "X" :human, "O" :hard, :board-size :3x3, :board [0 1 2 3 4 5 6 7 8]}]
        (it "returns ai's turn also when its human vs ai"
          (should= ["X" 1 2 3 "O" 5 6 7 8] (:board (sut/get-state old-state "1"))))))
    )

  (context "build-response"
    (it "should ask user if they'd like to play again"
        (should-contain "Continue last game?" (String. ^bytes (.handle
                                                         ttt-route-handler
                                                         (HttpRequest.
                                                           (ByteArrayInputStream.
                                                             (.getBytes
                                                               (str "GET /TTT HTTP/1.1\r\n"))))))))

    (it "returns a tic-tac-toe header"
      (should-contain "<h1>tic-tac-toe</h1>" (String. ^bytes (sut/build-response (initializer/parse-args ["--tui"])))))

    (it "returns a single printable wrapped in <h3>"
      (should-contain "<h3>Who will play as X?</h3>" (String. ^bytes (sut/build-response {:printables ["Who will play as X?"]}))))

    (it "returns multiple printables wrapped in <h3>"
      (let [state {:printables ["Who will play as X?" "1. Human" "2. Computer"]}]
        (should-contain "<h3>Who will play as X?" (String. ^bytes (sut/build-response state)))
        (should-contain "<h3>1. Human</h3>" (String. ^bytes (sut/build-response state)))
        (should-contain "<h3>2. Computer</h3>" (String. ^bytes (sut/build-response state)))))

    (it "contains a form that lets user submit selection"
      (should-contain "<form action=\"/ttt\" method=\"POST\">" (String. ^bytes (sut/build-response {})))
      (should-contain "<input type=\"number\" id=\"selection\" name=\"selection\"><br><br>" (String. ^bytes (sut/build-response {})))
      (should-contain "<input type=\"submit\" value=\"Submit\">" (String. ^bytes (sut/build-response {})))
      (should-contain "</form>" (String. ^bytes (sut/build-response {}))))

    (it "adds a state cookie in the header"
      (should-contain "\r\nSet-Cookie: state={}; Path=/; HttpOnly" (first (str/split (String. ^bytes (sut/build-response {})) #"\r\n\r\n")))
      (should-contain "\r\nSet-Cookie: state={:printables [\"hello\"]}; Path=/; HttpOnly" (first (str/split (String. ^bytes (sut/build-response {:printables ["hello"]})) #"\r\n\r\n")))
      )
    )

  )

