(ns d3-vis-clj.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [d3-vis-clj.core-test]
   [d3-vis-clj.common-test]))

(enable-console-print!)

(doo-tests 'd3-vis-clj.core-test
           'd3-vis-clj.common-test)
