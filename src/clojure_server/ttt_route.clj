(ns clojure-server.ttt-route
  (:import (com.cleanCoders RouteHandler ResponseBuilder))
  (:require [tic-tac-toe.board :as board]
            [tic-tac-toe.play-game :as game]
            [tic-tac-toe.moves.core :as moves]
            [tic-tac-toe.printables :as printables]
            [tic-tac-toe.state-initializer :as initializer]
            [clojure.string :as str]))

(defn- maybe-get-form [state]
  (if (:end-game? state)
    ""
    (str "<form action=\"/ttt\" method=\"POST\">"
         "<input type=\"number\" id=\"selection\" name=\"selection\"><br><br>"
         "<input type=\"submit\" value=\"Submit\">"
         "</form>")))

(defn- ->html [printable]
  (apply str (mapv #(str "<h3>" % "</h3>") printable)))

(defn get-all-boards
  ([state] (get-all-boards (:move-order state) (board/create-board (:board-size state)) []))

  ([move-order board result]
   (if (empty? move-order)
     result
     (let [updated-board (board/update-board (first move-order) board)]
       (recur (rest move-order) updated-board (conj result updated-board))))))

(defn- ai-v-ai? [state]
  (and (not (= :human (state "X"))) (not (= :human (state "O")))))

(defn- all-boards-to-html [state]
  (->> (get-all-boards state)
       (mapv printables/get-board-printables)
       (mapv #(conj % "<br>"))
       (flatten)
       (->html)))

(defn- get-game-over-board [state]
  (cond (not (:game-over? state)) ""
        (ai-v-ai? state) (all-boards-to-html state)
        :else (->html (printables/get-board-printables (:board state)))))

(defn build-body [state]
  (str "<h1>tic-tac-toe</h1>\n"
       (get-game-over-board state)
       (->html (:printables state))
       (maybe-get-form state)))

(defn- get-base-response [state]
  (let [body (build-body state)]
    (str/split
      (String.
        (.buildResponse (ResponseBuilder.) (.getBytes body))) #"\r\n\r\n")))

(defn- add-cookie [state base-response]
  (.getBytes (str (first base-response)
                  (str "\r\nSet-Cookie: state=" state "; Path=/; HttpOnly\r\n\r\n")
                  (second base-response))))

(defn build-response [state]
  (let [base-response (get-base-response state)]
    (add-cookie state base-response)))

(defn- human-turn? [old-state]
  (and (:board old-state)
       (= :human (moves/get-move-param old-state))
       (not (:game-over? old-state))))

(defn- get-selection [old-state selection]
  (cond
    (human-turn? old-state) (- (Integer/parseInt selection) 1)
    (and (:board old-state) (not (:game-over? old-state))) (moves/pick-move old-state)
    :else selection))

(defn get-state [old-state selection]
  (let [selection (get-selection old-state selection)
        updated-state (game/get-next-state old-state selection)]
    (if (or (human-turn? updated-state) (not (:board updated-state)) (:game-over? updated-state))
      updated-state
      (recur updated-state selection))))

(defn- split-cookie [cookie]
  (->> (str/split cookie #";")
       (filter #(str/includes? % "="))
       (filter #(not (str/includes? % "Path")))
       (mapv #(str/split (str/trim %) #"="))))

(defn parse-cookie
  ([cookie] (parse-cookie (split-cookie cookie) {}))

  ([cookies map]
   (if (empty? cookies)
     map
     (recur (rest cookies)
            (assoc map (keyword (first (first cookies))) (read-string (second (first cookies))))))))

(defn get-old-state [request]
  (:state (parse-cookie (.get request "Cookie"))))

(defn- handle-post-request [request]
  (let [body (.getBody request)
        old-state (get-old-state request)
        selection (second (str/split (String. ^bytes body) #"="))
        state (get-state old-state selection)]
    (build-response state)))

(defn- handle-get-request [_]
  (let [state (initializer/parse-args ["--tui"])]
    (build-response state)))

(deftype TttRouteHandler []
  RouteHandler

  (handle [_ request]
    (let [method (.get request "method")]
      (if (= method "POST")
        (handle-post-request request)
        (handle-get-request request)))))