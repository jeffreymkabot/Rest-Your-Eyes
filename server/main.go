package main

import (
	"context"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"runtime"
	"sync"
	"time"

	"github.com/go-ini/ini"
)

const defaultInterval = 1200000

type app interface {
	notify(string, string) error
	close()
}

type prefs struct {
	srcfile string

	// reads on GET /interval could race with writes on PATCH /interval
	mu       sync.Mutex
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
	err = existing.ReflectFrom(p)
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

func cron(data *prefs, updates <-chan int64, queries chan<- time.Time, app app) {
	d := data.Interval
	var end time.Time
	var timer <-chan time.Time
	reset := func(ms int64) {
		timer = time.After(time.Duration(ms) * time.Millisecond)
		end = time.Now().Add(time.Duration(ms) * time.Millisecond)
	}
	reset(d)

	for {
		select {
		case <-timer:
			reset(d)
			log.Print("Rest your eyes :))")
			if app != nil {
				app.notify("Rest your eyes", ":))")
			}
		case d = <-updates:
			reset(d)
			data.mu.Lock()
			data.Interval = d
			data.mu.Unlock()
			data.save()
		case queries <- end:
		}
	}
}

func main() {
	cfg := flag.String("f", "Prefs.ini", "config file")
	flag.Parse()
	log.Printf("prefs from file %v", *cfg)

	data, _ := load(*cfg)
	log.Printf("initial interval %vms", data.Interval)
	log.Printf("os %v", runtime.GOOS)

	var app app
	var err error
	if runtime.GOOS == "windows" {
		app, err = NewWindowsApp()
		if err != nil {
			log.Fatal(err)
		}
		defer app.close()
	}

	updates := make(chan int64)
	queries := make(chan time.Time)
	go cron(data, updates, queries, app)

	mux := http.NewServeMux()
	mux.HandleFunc("/interval", intervalHandler(data, updates))
	mux.HandleFunc("/remaining", remainingHandler(queries))
	server := &http.Server{Addr: "localhost:8080", Handler: mux}
	go server.ListenAndServe()

	// need to gracefully shutdown server to yield os system tray resources
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)
	<-c
	server.Shutdown(context.Background())
}
