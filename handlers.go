package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/websocket"
)

var upgrader = &websocket.Upgrader{}

func prefsHandler(data *prefs, patch chan<- *prefs) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case "GET":
			data.mu.Lock()
			obj, err := json.Marshal(data)
			data.mu.Unlock()
			if err != nil {
				http.Error(w, fmt.Sprintf("err %v", err), http.StatusInternalServerError)
			}
			w.Header().Add("Content-Type", "application/json")
			fmt.Fprintf(w, "%s", obj)
			return
		case "PATCH":
			tmp := &prefs{}
			decoder := json.NewDecoder(r.Body)
			if err := decoder.Decode(tmp); err != nil {
				http.Error(w, fmt.Sprintf("err %v", err), http.StatusBadRequest)
				return
			}
			if err := tmp.isValid(); err != nil {
				http.Error(w, fmt.Sprintf("err %v", err), http.StatusBadRequest)
				return
			}
			select {
			case patch <- tmp:
			case <-time.After(1 * time.Second):
				http.Error(w, "timeout", http.StatusRequestTimeout)
				return
			}
			w.WriteHeader(http.StatusAccepted)
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
				fmt.Fprintf(w, "%v", int64(time.Until(end)/time.Millisecond))
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

func websocketHandler(clients *[]*websocket.Conn) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		ws, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			log.Printf("error upgrade connection %v", err)
			return
		}
		// add ws to list of clients
		log.Printf("upgraded connection %#v", ws)
		*clients = append(*clients, ws)
	}
}
