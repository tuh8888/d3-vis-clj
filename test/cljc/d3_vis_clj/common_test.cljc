(ns d3-vis-clj.common-test
  #? (:cljs (:require-macros [cljs.test :refer (is deftest testing)]))
  (:require [d3-vis-clj.common :as sut]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test])))

(deftest example-passing-test-cljc
  (is (= 1 1)))
