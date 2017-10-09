package main

import (
	"fmt"
	"net/http"
	"strconv"
	"time"
)

func intervalHandler(data *prefs, updates chan<- int64) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case "GET":
			data.mu.Lock()
			fmt.Fprintf(w, "%v", data.Interval)
			data.mu.Unlock()
			return
		case "PATCH":
			d, err := strconv.Atoi(r.FormValue("interval"))
			if err != nil {
				http.Error(w, fmt.Sprintf("err %v", err), http.StatusBadRequest)
				return
			}
			select {
			case updates <- int64(d):
				w.WriteHeader(http.StatusAccepted)
			case <-time.After(1 * time.Second):
				http.Error(w, "timeout", http.StatusRequestTimeout)
			}
			return
		default:
			http.Error(w, "unsupported method", http.StatusMethodNotAllowed)
			return
		}
	}
}

func remainingHandler(queries <-chan time.Time) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case "GET":
			select {
			case end := <-queries:
				fmt.Fprintf(w, "%v", time.Until(end))
			case <-time.After(1 * time.Second):
				http.Error(w, "timeout", http.StatusRequestTimeout)
			}
			return
		default:
			http.Error(w, "unsupported method", http.StatusMethodNotAllowed)
			return
		}
	}
}
