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

func (p *prefs) save() error {
	return p.saveTo(p.srcfile)
}

func (p *prefs) saveTo(file string) error {
	// load existing cfg and append with p
	// to prevent erasing any values not in p
	existing, err := ini.Load(file)
	if err != nil {
		return err
	}
	p.mu.RLock()
	err = existing.ReflectFrom(p)
	p.mu.RUnlock()
	if err != nil {
		return err
	}

	f, err := os.OpenFile(file, os.O_TRUNC|os.O_WRONLY, 0600)
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
	log.Printf("prefs file %v", *cfg)

	prefs := &prefs{
		srcfile:  *cfg,
		Interval: defaultInterval,
	}

	err := ini.MapTo(prefs, *cfg)
	if err != nil {
		log.Fatal(err)
	}
	log.Printf("starting interval %vms", prefs.Interval)

	updates := make(chan int64)
	go cron(prefs, updates)

	http.HandleFunc("/interval", func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case "GET":
			prefs.mu.RLock()
			fmt.Fprintf(w, "%v", prefs.Interval)
			prefs.mu.RUnlock()
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
