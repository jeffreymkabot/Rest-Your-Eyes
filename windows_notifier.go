package main

import "github.com/lxn/walk"

type windowsApp struct {
	main *walk.MainWindow
	ni   *walk.NotifyIcon
}

func NewWindowsApp() (*windowsApp, error) {
	app := &windowsApp{}
	main, err := walk.NewMainWindow()
	if err != nil {
		return nil, err
	}
	app.main = main

	icon, err := walk.NewIconFromFile("./rsrc/icons/package/windows/RestYourEyes.ico")
	if err != nil {
		return nil, err
	}

	ni, err := walk.NewNotifyIcon()
	if err != nil {
		return nil, err
	}
	app.ni = ni

	if err := ni.SetIcon(icon); err != nil {
		ni.Dispose()
		return nil, err
	}

	if err := ni.SetVisible(true); err != nil {
		ni.Dispose()
		return nil, err
	}
	
	return app, nil
}

func (app *windowsApp) notify(title string, msg string) error {
	return app.ni.ShowMessage(title, msg)
}

func (app *windowsApp) close() {
	app.ni.Dispose()
}
