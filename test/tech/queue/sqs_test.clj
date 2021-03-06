(ns tech.queue.sqs-test
  (:require [clojure.test :refer :all]
            [tech.queue.protocols :as q]
            [tech.queue.worker-test :as worker-test]
            [com.stuartsierra.component :as c]
            [tech.queue.sqs :as sqs]
            [tech.queue.auth :as auth]
            [tech.io.uuid :as uuid]
            [tech.queue.resource-limit :as resource-limit])
  (:import [java.util UUID]))



(defmacro with-temp-sqs-queue
  [& body]
  `(let [queue-prefix# (str "dev-" (uuid/random-uuid-str))
         auth-provider# (c/start (auth/vault-aws-auth-provider
                                  "aws/sts/core"
                                  (sqs/provider queue-prefix# {:tech.aws/endpoint "us-west-2"})
                                  {}))
         temp-provider# (c/start (sqs/temp-provider auth-provider#))
         ~'queue (q/get-or-create-queue! temp-provider# :test {})]
     (try
       ~@body
       (finally
         (c/stop temp-provider#)
         (c/stop auth-provider#)))))


(deftest process-item
  (with-temp-sqs-queue
    (worker-test/test-process-item queue nil)))


(deftest process-item-res-mgr
  (with-temp-sqs-queue
    (worker-test/test-process-item
     queue
     (resource-limit/resource-manager {:initial-resources
                                       {:froodles 10}}))))


(deftest msg-lifetime
  (with-temp-sqs-queue
    (worker-test/test-msg-lifetime queue nil)))


(deftest msg-lifetime-res-mgr
  (with-temp-sqs-queue
    (worker-test/test-msg-lifetime
     queue
     (resource-limit/resource-manager {:initial-resources
                                       {:froodles 10}}))))


(deftest msg-not-ready-timeout
  (with-temp-sqs-queue
    (worker-test/test-msg-not-ready-timeout queue nil)))


(deftest msg-not-ready-timeout-res-mgr
  (with-temp-sqs-queue
    (worker-test/test-msg-not-ready-timeout
     queue
     (resource-limit/resource-manager {:initial-resources
                                       {:froodles 10}}))))
