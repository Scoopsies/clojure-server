(ns clojure-server.ttt-route-spec
  (:require [clojure.string :as str]
            [speclj.core :refer :all]
            [clojure-server.ttt-route :as sut]
            [tic-tac-toe.board :as board]
            [tic-tac-toe.data.data-io :as data-io]
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

  (before (reset! data-io/data-store :memory))

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
    (context "set-up menus"
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
          (should= ["X" 1 2 3 "O" 5 6 7 8] (:board (sut/get-state old-state "1"))))
        ))

    (context "ai v ai"
      (let [old-state {:ui :tui "O" :hard "X" :hard}]
        (it "returns a played game of ai"
          (should= ["X" "O" "X" "X" "O" "O" "O" "X" "X"] (:board (sut/get-state old-state "1"))))))

    (context "play again menu"
      (it "updates to a new game if user selects 1"
        (sut/get-state {"X" :human "O" :human :board ["X" "X" 2 "O" "O" 5 6 7 8]} "3")
        (let [old-state {:game-over? true :ui :tui "O" :hard "X" :human :board [0 1 2 3 4 5 6 7 8]}
              result {:ui :tui, :printables ["Who will play as X?" "1. Human" "2. Computer Easy" "3. Computer Medium" "4. Computer Hard"], :game-over? false}]
          (should= result (sut/get-state old-state "1"))))

      (it "ends game if user selects 2"
        (let [old-state {:game-over? true :ui :tui "O" :hard "X" :human :board [0 1 2 3 4 5 6 7 8]}
              result {:end-game? true, :printables ["See you next time!"]}]
          (should= result (sut/get-state old-state "2")))))
    )

  (context "build-response"
    (it "should ask user if they'd like to play again"
      (sut/get-state {"X" :human "O" :human :board ["X" "X" 2 "O" 4 5 6 7 8]} "3")
        (should-contain "Continue last game?" (String. ^bytes (.handle
                                                         ttt-route-handler
                                                         (HttpRequest.
                                                           (ByteArrayInputStream.
                                                             (.getBytes
                                                               (str "GET /TTT HTTP/1.1\r\n"))))))))

    (it "returns a tic-tac-toe header"
      (should-contain "<h1>tic-tac-toe</h1>" (String. ^bytes (sut/build-response (initializer/parse-args ["--tui"])))))

    (it "returns a single printable wrapped in <h3>"
      (let [state {:printables ["Who will play as X?"]}]
        (should-contain "<h3>Who will play as X?</h3>" (String. ^bytes (sut/build-response state)))
        (should-not-contain "<h3>1. Human</h3>" (String. ^bytes (sut/build-response state)))))

    (it "returns multiple printables wrapped in <h3>"
      (let [state {:printables ["Who will play as X?" "1. Human" "2. Computer"]}]
        (should-contain "<h3>Who will play as X?" (String. ^bytes (sut/build-response state)))
        (should-contain "<h3>1. Human</h3>" (String. ^bytes (sut/build-response state)))
        (should-contain "<h3>2. Computer</h3>" (String. ^bytes (sut/build-response state)))))

    (it "contains a form that lets user submit selection"
      (let [state {}]
        (should-contain "<form action=\"/ttt\" method=\"POST\">" (String. ^bytes (sut/build-response state)))
        (should-contain "<input type=\"number\" id=\"selection\" name=\"selection\"><br><br>" (String. ^bytes (sut/build-response state)))
        (should-contain "<input type=\"submit\" value=\"Submit\">" (String. ^bytes (sut/build-response state)))
        (should-contain "</form>" (String. ^bytes (sut/build-response state)))))

    (it "does not contain a form that lets user submit selection if end-game? = true"
      (let [state {:end-game? true}]
        (should-not-contain "<form action=\"/ttt\" method=\"POST\">" (String. ^bytes (sut/build-response state)))
        (should-not-contain "<input type=\"number\" id=\"selection\" name=\"selection\"><br><br>" (String. ^bytes (sut/build-response state)))
        (should-not-contain "<input type=\"submit\" value=\"Submit\">" (String. ^bytes (sut/build-response state)))
        (should-not-contain "</form>" (String. ^bytes (sut/build-response state)))))

    (it "adds a state cookie in the header"
      (should-contain "\r\nSet-Cookie: state={}; Path=/; HttpOnly" (first (str/split (String. ^bytes (sut/build-response {})) #"\r\n\r\n")))
      (should-contain "\r\nSet-Cookie: state={:printables [\"hello\"]}; Path=/; HttpOnly" (first (str/split (String. ^bytes (sut/build-response {:printables ["hello"]})) #"\r\n\r\n"))))
    )

  (context "build-body"
    (it "displays all of the boards played on the game-over screen if ai v ai"
      (let [state {:game-over? true, :board-size :3x3, :printables ["X wins!" "" "Play Again?" "1. Yes" "2. No"], :ui :tui, :id 124, "O" :hard, :move-order [0 1 3 4 6], "X" :hard, :board ["X" "O" 2 "X" "O" 5 "X" 7 8]}]
        (should-contain (str "<h3>" (first (printables/get-board-printables (:board state))) "</h3>") (sut/build-body state))
        (should-contain (str "<h3>" (first (printables/get-board-printables ["X" 1 2 3 4 5 6 7 8])) "</h3>") (sut/build-body state))))

    (it "displays only the final board if there was a human player"
      (let [state {:game-over? true, :board-size :3x3, :printables ["X wins!" "" "Play Again?" "1. Yes" "2. No"], :ui :tui, :id 124, "O" :hard, :move-order [0 1 3 4 6], "X" :human, :board ["X" "O" 2 "X" "O" 5 "X" 7 8]}]
        (should-contain (str "<h3>" (first (printables/get-board-printables (:board state))) "</h3>") (sut/build-body state))
        (should-not-contain (str "<h3>" (first (printables/get-board-printables ["X" 1 2 3 4 5 6 7 8])) "</h3>") (sut/build-body state))))
    )

  (context "get-all-boards"
    (it "returns list of one move game boards on a :3x3"
      (should= [["X" 1 2 3 4 5 6 7 8]] (sut/get-all-boards {:board-size :3x3 :move-order [0]}))
      (should= [[0 "X" 2 3 4 5 6 7 8]] (sut/get-all-boards {:board-size :3x3 :move-order [1]})))

    (it "returns list of one move game boards on a :4x4"
      (should= [(board/update-board 0 (range 16))] (sut/get-all-boards {:board-size :4x4 :move-order [0]})))

    (it "returns a list of two move game boards"
      (should= [["X" 1 2 3 4 5 6 7 8] ["X" "O" 2 3 4 5 6 7 8]] (sut/get-all-boards {:board-size :3x3 :move-order [0 1]})))
    )
  )

