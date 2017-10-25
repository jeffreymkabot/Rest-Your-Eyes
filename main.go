package main

import (
	"context"
	"errors"
	"flag"
	"log"
	"net/http"
	"os"
	"os/signal"
	"runtime"
	"sync"
	"time"

	"github.com/go-ini/ini"
	"github.com/gorilla/websocket"
)

const defaultInterval = 1200000

type app interface {
	notify(string, string) error
	close()
}

type prefs struct {
	srcfile string

	// reads on GET /prefs could race with writes on PATCH /prefs
	mu                  sync.Mutex
	Interval            int64
	DarkTheme           int8
	AggressiveReminders int8
}

// will return default prefs on any error
func load(cfg string) (*prefs, error) {
	prefs := &prefs{
		srcfile:  cfg,
		Interval: defaultInterval,
	}
	err := ini.MapTo(prefs, cfg)
	if os.IsNotExist(err) {
		err = nil
	}
	return prefs, err
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

func (p *prefs) isValid() error {
	if p.Interval <= 0 {
		return errors.New("interval must be positive")
	}
	if p.DarkTheme != 0 && p.DarkTheme != 1 {
		return errors.New("dark theme must be 0 or 1")
	}
	return nil
}

func cron(data *prefs, patch <-chan *prefs, remaining chan<- time.Time, app app) {
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
		case remaining <- end:
		case <-timer:
			reset(d)
			log.Print("Rest your eyes :))")
			if app != nil {
				app.notify("Rest your eyes", ":))")
			}
		case tmp := <-patch:
			// tmp validated upstream
			changed := false
			data.mu.Lock()
			if tmp.Interval != d {
				d = tmp.Interval
				data.Interval = d
				changed = true
				reset(d)
			}
			if tmp.DarkTheme != data.DarkTheme {
				data.DarkTheme = tmp.DarkTheme
				changed = true
			}
			data.mu.Unlock()
			if changed {
				data.save()
			}
		}
	}
}

func main() {
	cfg := flag.String("f", "./client/rsrc/Prefs.ini", "config file")
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

	patch := make(chan *prefs)
	remaining := make(chan time.Time)
	// TODO make manipulations of the connection slice thread-safe
	// TODO kill stale connections
	// TODO notify active clients when timer goes off
	clients := []*websocket.Conn{}
	go cron(data, patch, remaining, app)

	mux := http.NewServeMux()
	mux.HandleFunc("/prefs", prefsHandler(data, patch))
	mux.HandleFunc("/remaining", remainingHandler(remaining))
	mux.HandleFunc("/websocket", websocketHandler(&clients))
	server := &http.Server{Addr: "localhost:8080", Handler: mux}
	go server.ListenAndServe()

	// need to gracefully shutdown server to yield os system tray resources
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt)
	<-c

	ctx, end := context.WithTimeout(context.Background(), 1*time.Second)
	log.Print(server.Shutdown(ctx))
	end()
}
