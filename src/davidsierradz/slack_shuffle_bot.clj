(ns davidsierradz.slack-shuffle-bot
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.logger :as logger]
            [compojure.core :refer [POST]]
            [clojure.string :refer [join split]]
            [clojure.java.io :refer [copy]])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec))
  (:gen-class))

;; Stolen from
;; https://github.com/noisesmith/groundhog/blob/c50b79e6500e632ff77af229bdb51c76973f9e5b/src/noisesmith/groundhog.clj#L67
(defn tee-stream
  "Given a stream we can read from, returns the eagerly read bytes of the stream,
  plus a new stream that will provide those same contents."
  [stream]
  (let [buffer (ByteArrayOutputStream.)
        _ (copy stream buffer)
        bytes (.toByteArray buffer)]
    {:stream (ByteArrayInputStream. bytes), :contents bytes}))

(def slack-signing-secret (System/getenv "SLACK_SIGNING_SECRET"))

;; Inspired by https://gist.github.com/jhickner/2382543#file-1-clj
(defn hmac
  "Calculate HMAC signature for given data."
  [^String key ^String data]
  (let [hmac-sha1 "HmacSHA256"
        signing-key (SecretKeySpec. (.getBytes key) hmac-sha1)
        mac (doto (Mac/getInstance hmac-sha1) (.init signing-key))]
    (apply str (map #(format "%02x" %) (.doFinal mac (.getBytes data))))))

(defn wrap-extract-body-middleware
  [handler]
  (fn [request]
    (let [{body :stream, contents :contents} (tee-stream (:body request))
          duplicated-request (assoc request
                                    :body body
                                    :contents contents)]
      (handler duplicated-request))))

(defn wrap-verify-request-middleware
  [handler]
  (fn [request]
    (if (= (str
            "v0="
            (hmac slack-signing-secret
                  (str "v0:" (get (:headers request)
                                  "x-slack-request-timestamp")
                       ":" (apply str (map #(char %) (:contents request))))))
           (get (:headers request) "x-slack-signature"))
      (handler request)
      {:status 403})))

(def inner-handler
  (-> (POST
        "/"
        request
        (let [text (get-in request [:params "text"])]
          {:status 200,
           :headers {"Content-Type" "application/json"},
           :body (str "{\n  \"response_type\": \"in_channel\",\n  \"text\": \""
                      "Date: "
                      (.toString (java.time.ZonedDateTime/now))
                      "\n\nItems:\n\n"
                      (join " \n"
                            (first (reduce (fn [acc v]
                                             (let [i (inc (last acc))]
                                               (conj [(conj (first acc)
                                                            (str i ". " v))]
                                                     i)))
                                           [[] 0]
                                           (shuffle (split text #" ")))))
                      "\"\n}")}))
      (logger/wrap-log-request-params {:transform-fn #(assoc % :level :info)})
      wrap-params
      wrap-verify-request-middleware
      wrap-extract-body-middleware))

(defn -main
  []
  (run-jetty inner-handler
             {:port (Integer/parseInt (or (System/getenv "PORT") "3000"))}))
