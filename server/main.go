package main

import (
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"strconv"
	"sync"
	"time"

	"github.com/go-ini/ini"
	"gopkg.in/toast.v1"
)

const defaultInterval = 1200000

type prefs struct {
	srcfile  string

	mu       sync.RWMutex
	Interval int64
}

func load(cfg string) (*prefs, error) {
	prefs := &prefs{
		srcfile:  cfg,
		Interval: defaultInterval,
	}
	err := ini.MapTo(prefs, cfg)
	if err == nil || os.IsNotExist(err) {
		return prefs, nil
	}
	return nil, err
}

func (p *prefs) save() error {
	return p.saveTo(p.srcfile)
}

func (p *prefs) saveTo(file string) error {
	// load existing cfg (if exists) and append with p
	// to prevent erasing any values not mapped in p
	existing := ini.Empty()
	err := existing.Append(file)
	if err != nil && !os.IsNotExist(err) {
		return err
	}
	p.mu.RLock()
	err = existing.ReflectFrom(p)
	p.mu.RUnlock()
	if err != nil {
		return err
	}

	f, err := os.OpenFile(file, os.O_CREATE|os.O_TRUNC|os.O_WRONLY, 0600)
	if err != nil {
		return err
	}
	_, err = existing.WriteTo(f)
	f.Close()
	return err
}

func main() {
	cfg := flag.String("f", "Prefs.ini", "config file")
	flag.Parse()
	log.Printf("prefs from file %v", *cfg)

	data, err := load(*cfg)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("starting interval %vms", data.Interval)

	updates := make(chan int64)
	go cron(data, updates)

	http.HandleFunc("/interval", func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case "GET":
			data.mu.RLock()
			fmt.Fprintf(w, "%v", data.Interval)
			data.mu.RUnlock()
		case "PATCH":
			d, err := strconv.Atoi(r.FormValue("interval"))
			if err != nil {
				fmt.Fprintf(w, "err %v", err)
				w.WriteHeader(http.StatusBadRequest)
				return
			}
			updates <- int64(d)
			w.WriteHeader(http.StatusAccepted)
			return
		default:
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}
	})
	http.ListenAndServe(":8080", nil)
}

func cron(data *prefs, updates <-chan int64) {
	var timer <-chan time.Time
	var d int64
	var ok bool
	for {
		data.mu.RLock()
		timer = time.After(time.Duration(data.Interval) * time.Millisecond)
		data.mu.RUnlock()
		select {
		case d, ok = <-updates:
			if !ok {
				return
			}
			data.mu.Lock()
			data.Interval = d
			data.mu.Unlock()
			data.save()
		case <-timer:
			fmt.Println("Rest your eyes :))")
			notice("8)", "take a break").Push()
		}
	}
}

func notice(title string, msg string) *toast.Notification {
	return &toast.Notification{
		AppID:               "rest your eyes",
		Title:               title,
		Message:             msg,
		Duration:            toast.Short,
		Audio:               toast.Silent,
		ActivationArguments: "http://localhost:8080/interval",
	}
}
