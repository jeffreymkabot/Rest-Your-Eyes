// Author: Sean Pesce

package restyoureyes;

import static restyoureyes.Constants.*;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.AccessDeniedException;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;
import javax.imageio.IIOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 * This class represents an application that reminds the user to take a break from
 *  screen time to avoid eye strain. A reminder is sent every time the configurable
 *  interval period expires.
 *
 * @author Sean Pesce
 */
public class RestYourEyes extends Application {

    // Configurable settings
    public static boolean   hideOnStartup = DEFAULT_STARTUP_HIDE,
                            remindWithDialog = DEFAULT_REMIND_WITH_DIALOG,
                            useDarkTheme = DEFAULT_USE_DARK_THEME;

    public static long interval = DEFAULT_INTERVAL;

    public static double windowWidth = DEFAULT_WINDOW_WIDTH;
    public static double windowHeight = DEFAULT_WINDOW_HEIGHT;


    // User Interface nodes
    public static Stage stage;
    public static Scene scene;
    public static Group group;
    public static VBox primaryVBox;
    public static MenuBar fileToolbar;
        public static Menu fileMenu;
            public static MenuItem fileHide;
            public static MenuItem fileExit;
        public static Menu optionsMenu;
            public static MenuItem toggleReminders;
            public static MenuItem toggleTheme;
        public static Menu advancedMenu;
            public static Menu configMenu;
                public static MenuItem editConfig;
                public static MenuItem reloadConfig;
    public static VBox nextReminderVBox;
    public static Label nextReminderLbl;
    public static Label timeRemainingLbl;
    public static Tooltip timeRemainingLblTooltip;
    public static HBox setIntervalHBox;
    public static TextField newIntervalField;
    public static ComboBox<String> unitPicker;
    public static Button setIntervalBt;

    public static Alert reminder;
    public static HBox reminderBtPane;
    public static Button disableRemindersBt;

    public static java.awt.TrayIcon trayIcon;
    public static java.awt.MenuItem toggleRemindersTrayItem;

    // Variables
    public static boolean   runLoop = true,
                            disableReminders = false,
                            sysTraySupported = true;
    public static long newInterval = 0;
    public static Instant nextReminder;


    /**
     * @param args
     *  The list of command line arguments passed to the program.
     */
    public static void main(String[] args) {

        try{

    		launch(args);


        }catch(Exception ex){
        	// Catch and print any exceptions to error log
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String stackTrace = sw.toString(); // stack trace as a string
            try {
                writeTextFile("error.log", stackTrace, true);
            } catch (IOException ex1) {
                //Logger.getLogger(RestYourEyesUtil.class.getName()).log(Level.SEVERE, null, ex1);
            }
            ex.printStackTrace();
        }
    }


    /**
     * The main entry point for all JavaFX applications. The start method is called
     *  after the init method has returned, and after the system is ready for the
     *  application to begin running.
     *
     * <p>
     * NOTE: This method is called on the JavaFX Application Thread.
     * </p>
     *
     * @param newStage
     *  The stage supplied for the primary window of the program.
     *
     * @throws java.lang.Exception
     *
     */
    @Override
    public void start(Stage newStage) throws Exception {

    	try{
            System.out.println("hello world " + Client.getRemaining());
    		stage = newStage;
            getUserPrefs();

            initWindow();

            initSystemTray();

            initEventHandlers();


            Thread timerThread = new Thread(() -> {
                timerLoop();
            });
            timerThread.start();

            if(hideOnStartup && sysTraySupported){
                // Wait for tray icon to initialize (or fail, meaning system tray is not supported)
                while(sysTraySupported && trayIcon == null){
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        System.out.println("Sleep interrupted.");
                    }
                }

                if(sysTraySupported){
                    Instant nowStart = Instant.now();
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        if(!disableReminders)
                            trayIcon.displayMessage(PROGRAM_TITLE, "Reminding every " + getDurationAsString(Duration.between(nowStart, nowStart.plusMillis(interval)), false), java.awt.TrayIcon.MessageType.INFO);
                        else
                            trayIcon.displayMessage(PROGRAM_TITLE, "Reminders disabled", java.awt.TrayIcon.MessageType.INFO);
                    });
                }else{
                    stage.show();
                }
            }else{
                stage.show();
            }

            updateElements();


        }catch(Exception ex){
        	// Catch and print any exceptions to error log
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            String stackTrace = sw.toString(); // stack trace as a string
            try {
                writeTextFile("error.log", stackTrace, true);
            } catch (IOException ex1) {
                //Logger.getLogger(RestYourEyesUtil.class.getName()).log(Level.SEVERE, null, ex1);
            }
            ex.printStackTrace();
        }

    }


    /**
     * Reads user preferences from the configuration file.
     */
    public static void getUserPrefs(){
        System.out.println("Loading startup configuration...");
        File configFile = new File(CONFIG_FILE);

        if(!configFile.exists()){
            // Create default configuration file
            System.out.println("ERROR: Unable to locate configuration file. Creating new file with default settings...");
            writeCurrentConfiguration();
        }

        Scanner fileReader;
        try {
            fileReader = new Scanner(readTextFile(CONFIG_FILE));

            // Read config file 1 line at a time
            while(fileReader.hasNextLine()){
                String line = fileReader.nextLine().trim();

                if(isConfigKeyValue(line, STARTUP_HIDE_KEY)){
                    switch(line.charAt(STARTUP_HIDE_KEY.length())){
                        case '1':
                            hideOnStartup = true;
                            break;
                        case '0':
                            hideOnStartup = false;
                            break;
                        default:
                            break;
                    }
                }else if(isConfigKeyValue(line, REMIND_WITH_DIALOG_KEY)){
                    switch(line.charAt(REMIND_WITH_DIALOG_KEY.length())){
                        case '1':
                            remindWithDialog = true;
                            break;
                        case '0':
                            remindWithDialog = false;
                            break;
                        default:
                            break;
                    }
                }else if(isConfigKeyValue(line, INTERVAL_KEY)){
                    try{
                        long tmpNewInterval = Long.parseLong(getConfigKeyValue(line, INTERVAL_KEY));
                        interval = tmpNewInterval;
                    }catch(NumberFormatException nfE){
                        // Value could not be parsed as a long integer
                    }
                }else if(isConfigKeyValue(line, DARK_THEME_KEY)){
                    switch(line.charAt(DARK_THEME_KEY.length())){
                        case '1':
                            useDarkTheme = true;
                            break;
                        case '0':
                            useDarkTheme = false;
                            break;
                        default:
                            break;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // Unable to parse user preferences; use default settings
            System.out.println("ERROR: Unable to parse configuration file. Using default settings instead.");
        }
    }


    /**
     * Initializes the main program window.
     */
    public static void initWindow(){
        System.out.println("Initializing main window...");

        // Don't exit when all stages are closed
        Platform.setImplicitExit(false);

        stage.setWidth(windowWidth);
        stage.setHeight(windowHeight);

        group = new Group();
        scene = new Scene(group);
        scene.getStylesheets().add(DEFAULT_STYLE_SHEET);
        if(useDarkTheme)
            scene.getStylesheets().add(DARK_STYLE_SHEET);

        fileToolbar = new MenuBar();
        fileMenu = new Menu("File");
        fileHide = new MenuItem("Hide");
        fileExit = new MenuItem("Exit");
        fileMenu.getItems().addAll(fileHide, fileExit);
        optionsMenu = new Menu("Options");
        toggleReminders = new MenuItem("");
        if(!disableReminders)
            toggleReminders.setText("Disable reminders");
        else
            toggleReminders.setText("Enable reminders");
        toggleTheme = new MenuItem("");
        if(useDarkTheme)
            toggleTheme.setText("Use light color scheme");
        else
            toggleTheme.setText("Use dark color scheme");
        optionsMenu.getItems().addAll(toggleReminders, toggleTheme);
        advancedMenu = new Menu("Advanced");
        configMenu = new Menu("Configure");
        editConfig = new MenuItem("Edit startup config");
        reloadConfig = new MenuItem("Reload startup config");
        editConfig.setDisable(true);
        reloadConfig.setDisable(true);
        advancedMenu.getItems().add(configMenu);
        configMenu.getItems().addAll(editConfig, reloadConfig);
        fileToolbar.getMenus().addAll(fileMenu, optionsMenu, advancedMenu);


        nextReminderLbl = new Label("Next reminder:");
        nextReminderLbl.setStyle("-fx-font-size: 10px;");
        timeRemainingLbl = new Label("--:--:--.---");
        timeRemainingLbl.getStyleClass().add("timer");
        timeRemainingLbl.setPadding(new Insets(3.0, 10.0, 3.0, 10.0));
        timeRemainingLblTooltip = new Tooltip("Reminders are currently disabled.\nYou can enable reminders from the Options menu.");
        timeRemainingLblTooltip.getStyleClass().add("timer_tooltip");
        timeRemainingLbl.setTooltip(timeRemainingLblTooltip);
        nextReminderVBox = new VBox(nextReminderLbl, timeRemainingLbl);
        nextReminderVBox.setAlignment(Pos.CENTER);
        nextReminderVBox.setSpacing(1.0);

        newIntervalField = new TextField();
        newIntervalField.setPromptText("New interval");
        newIntervalField.getStyleClass().add("text_field");
        newIntervalField.setTooltip(new Tooltip("Interval must be between 1 minute and 10 hours"));
        unitPicker = new ComboBox<String>(FXCollections.observableArrayList(TIME_UNITS));
        unitPicker.setValue(TIME_UNITS[2]);
        unitPicker.setTooltip(new Tooltip("Change unit of time for setting new reminder interval"));
        setIntervalBt = new Button("Set");
        setIntervalBt.setTooltip(new Tooltip("Set new reminder cooldown time"));
        setIntervalBt.setDisable(true);
        setIntervalHBox = new HBox(newIntervalField, unitPicker, setIntervalBt);
        setIntervalHBox.setSpacing(5.0);
        setIntervalHBox.setAlignment(Pos.CENTER);

        primaryVBox = new VBox(nextReminderVBox, setIntervalHBox);
        primaryVBox.setAlignment(Pos.CENTER);
        primaryVBox.setSpacing(10.0);
        primaryVBox.setPrefWidth(stage.getWidth());
        primaryVBox.setPrefHeight(stage.getHeight());

        group.getChildren().addAll(primaryVBox, fileToolbar);

        stage.setScene(scene);
        stage.setTitle(PROGRAM_TITLE);
        try{
            stage.getIcons().add(new Image(ClassLoader.getSystemResourceAsStream(PROGRAM_ICON)));
        }catch(Exception ex){
            System.out.println("ERROR: Unable to load window icon");
        }

        primaryVBox.getStyleClass().add("main_window");

        initReminderWindow();

        timeRemainingLbl.requestFocus();

        primaryVBox.setPadding(new Insets(fileToolbar.getHeight(), 10.0, 10.0, 10.0));
    }


    /**
     * Sets up a system tray icon for the application.
     */
    private static void initSystemTray() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                // Initialize AWT toolkit
                Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();

                // Check for system tray support
                if (!java.awt.SystemTray.isSupported()) {
                    sysTraySupported = false;
                    System.out.println("ERROR: No system tray support.");
                    Platform.setImplicitExit(true);
                    return;
                }

                java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();

                // Set up system tray icon
                if(trayIcon != null)
                    java.awt.SystemTray.getSystemTray().remove(trayIcon);
                String iconRelPath = "style/" + PROGRAM_ICON_WHITE;
                if(!new File(iconRelPath).exists())
                    iconRelPath = PROGRAM_ICON_WHITE;
                trayIcon = new java.awt.TrayIcon(toolkit.createImage(iconRelPath));
                trayIcon.setImageAutoSize(true);

                // Show the main window on double-click
                trayIcon.addActionListener(event -> javafx.application.Platform.runLater(() -> {
                    stage.show();
                }));

                // Tray option to show the main window
                java.awt.MenuItem openItem = new java.awt.MenuItem("Open");
                openItem.addActionListener(event -> javafx.application.Platform.runLater(() -> {
                    stage.show();
                }));

                // Tray option to pause reminder notifications
                toggleRemindersTrayItem = new java.awt.MenuItem();

                toggleRemindersTrayItem.addActionListener(event -> {
                    toggleReminders();
                });

                // Convention for tray icons seems to be to set the default icon for opening the application stage in a bold font.
                java.awt.Font defaultFont = java.awt.Font.decode(null);
                java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
                openItem.setFont(boldFont);

                // To truly exit the application, the user must go to the system tray icon (or file menu)
                // and select the "exit" option; this will shutdown JavaFX and remove the
                // tray icon (removing the tray icon will also shut down AWT).
                java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
                exitItem.addActionListener(event -> {
                    runLoop = false;
                    javafx.application.Platform.runLater(() -> {
                        runLoop = false;
                        if(reminder.isShowing()){
                            ((Stage)reminder.getDialogPane().getScene().getWindow()).close();
                            reminder.close();
                        }
                        Platform.exit();
                    });
                    tray.remove(trayIcon);
                });

                // Set up the pop-up menu for the application
                final java.awt.PopupMenu popup = new java.awt.PopupMenu();
                popup.add(openItem);
                popup.add(toggleRemindersTrayItem);
                popup.addSeparator();
                popup.add(exitItem);
                trayIcon.setPopupMenu(popup);

                updateElements();

                // Add the application tray icon to the system tray
                tray.add(trayIcon);
            } catch (java.awt.AWTException ex) {
                sysTraySupported = false;
                System.out.println("ERROR: Unable to initialize system tray icon");
                Platform.setImplicitExit(true);
            }
        });
    }


    /**
     * Initializes the reminder dialog window.
     */
    public static void initReminderWindow(){

    	reminder = new Alert(Alert.AlertType.NONE, "Be sure to take a break from screen time to avoid eye strain!");
        HBox reminderBtPane = new HBox();
        Button disableRemindersBt = new Button("Disable reminders");

        reminder.setResizable(false);
        Stage remStage = (Stage)reminder.getDialogPane().getScene().getWindow();
        remStage.getScene().getStylesheets().add(DEFAULT_STYLE_SHEET);
        if(useDarkTheme)
            remStage.getScene().getStylesheets().add(DARK_STYLE_SHEET);

        try{
            remStage.getIcons().add(new Image(ClassLoader.getSystemResourceAsStream(PROGRAM_ICON)));
        }catch(Exception ex){
            System.out.println("ERROR: Unable to load reminder window icon");
        }
        remStage.setTitle("Rest your eyes!");
        DialogPane dPane = reminder.getDialogPane();
        dPane.setPrefWidth(350);
        //dPane.setMaxWidth(201);
        dPane.setPrefHeight(130);
        dPane.setMaxHeight(131);
        //remStage.setMaxWidth(201);
        remStage.setMaxHeight(131);
        dPane.setPadding(new Insets(0.0, 0.0, 0.0, 0.0));

        VBox layoutVBox = new VBox();
        layoutVBox.setAlignment(Pos.BOTTOM_CENTER);
        dPane.setContent(layoutVBox);
        Label msg = new Label("Be sure to take a break from screen time to avoid eye strain!");
        msg.setStyle("-fx-font-size: 12px;");
        msg.setTextAlignment(TextAlignment.CENTER);
        Button okBt = new Button("Ok");
        okBt.getStyleClass().add("reminder_button");
        disableRemindersBt.getStyleClass().add("reminder_button");
        reminderBtPane.getChildren().add(okBt);
        reminderBtPane.setSpacing(5.0);
        reminderBtPane.setAlignment(Pos.BOTTOM_RIGHT);
        layoutVBox.getChildren().addAll(msg, reminderBtPane);

        layoutVBox.setPrefWidth(dPane.getWidth());
        layoutVBox.setPrefHeight(dPane.getHeight());

        okBt.setOnAction(e -> {
            remStage.close();
            reminder.close();
        });

        // "Disable reminders" button in reminder dialog window
        disableRemindersBt.setOnAction(e -> {
            if(!disableReminders)
                toggleReminders();
        });

        remStage.setOnCloseRequest(e -> {
            //remStage.close();
            //reminder.close();
        });

        remStage.setAlwaysOnTop(true);
    }


    /**
     * Initializes the handler functions that respond to user interaction with the GUI.
     */
    public static void initEventHandlers(){

        fileToolbar.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldHeight, Number newHeight) -> {
            primaryVBox.setPadding(new Insets(fileToolbar.getHeight(), 10.0, 10.0, 10.0));
        });

        scene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldWidth, Number newWidth) -> {
            primaryVBox.setPrefWidth(scene.getWidth());
            fileToolbar.setPrefWidth(scene.getWidth());
        });
        primaryVBox.setPrefWidth(scene.getWidth());
        fileToolbar.setPrefWidth(scene.getWidth());


        scene.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldHeight, Number newHeight) -> {
            primaryVBox.setPrefHeight(scene.getHeight());
        });
        primaryVBox.setPrefHeight(scene.getHeight());


        scene.setOnMouseClicked((MouseEvent mouseEvent) -> {
            if(mouseEvent.getButton().equals(MouseButton.PRIMARY) && mouseEvent.getClickCount() == 2){
                timeRemainingLbl.requestFocus();
            }
        });


        newIntervalField.textProperty().addListener((ObservableValue<? extends String> observableValue, String oldText, String newText) -> {
            checkInput();
        });


        unitPicker.setOnAction(e -> {
            checkInput();
        });


        setIntervalBt.setOnAction(e -> {
            try{
                String inputString = newIntervalField.getText();
                newIntervalField.clear();
                timeRemainingLbl.requestFocus();
                long newInt = Long.parseLong(inputString);
                switch(FXCollections.observableArrayList(TIME_UNITS).indexOf(unitPicker.getValue())){
                    case HOUR:
                        newInt *= 60;
                    case MINUTE:
                        newInt *= 60;
                    case SECOND:
                        newInt *= 1000;
                    case MILLISECOND:
                    default:
                        break;
                }
                if(newInt < 60000 || newInt > 36000000)
                    throw new NumberFormatException();

                newInterval = newInt;

            }catch(NumberFormatException nfEx){
                System.out.println("ERROR: Invalid interval specified.");
            }
        });


        toggleReminders.setOnAction(e -> {
            toggleReminders();
        });


        toggleTheme.setOnAction(e -> {
            useDarkTheme = !useDarkTheme;
            updateElements();
            writeCurrentConfiguration();
        });


        fileHide.setOnAction(e -> {
            stage.hide();
        });


        editConfig.setOnAction(e -> {
            try {
                Desktop.getDesktop().open(new File(CONFIG_FILE));
            } catch (IOException e1) {
                System.out.println("ERROR: Failed to open configuration file.");
            }
        });


        reloadConfig.setOnAction(e -> {
            getUserPrefs();
            newInterval = interval;
            updateElements();
        });

        stage.setOnCloseRequest(e -> {
            if(trayIcon == null){
                runLoop = false;
                Platform.exit();
            }else{
                stage.hide();
            }
        });


        fileExit.setOnAction(e -> {
            runLoop = false;
            Platform.exit();
            if(trayIcon != null)
                java.awt.SystemTray.getSystemTray().remove(trayIcon);
        });
    }

    /**
     * Continuously runs a loop in a separate thread from the rest of the program
     *  and performs the following functions:
     *
     * <ul>
     *      <li>
     *      Updates the timer display.
     *      <\li>
     *      <li>
     *      Checks if the reminder timer has expired, at which point a reminder
     *       notification is sent to the user and the timer is restarted.
     *      <\li>
     * </ul>
     */
    public static void timerLoop(){

        System.out.println("Starting timer...");

        Instant now = Instant.now();
        nextReminder = now.plusMillis(interval);

        updateElements();

        while(runLoop){
            boolean paused = false;


            while(runLoop && disableReminders){
                if(!paused){
                    updateElements();
                    paused = true;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.out.println("Sleep interrupted.");
                }
            }
            if(!runLoop)
                continue;
            if(paused){
                now = Instant.now();
                nextReminder = now.plusMillis(interval);
            }


            now = Instant.now();
            Duration dur = Duration.between(now, nextReminder);
            long hours = dur.toHours();
            long minutes = dur.minusHours(hours).toMinutes();
            long seconds = dur.minusMinutes(minutes).getSeconds();
            long milliseconds = dur.minusMinutes(minutes).toMillis() - (seconds * 1000);

            if(milliseconds < 0 || seconds < 0 || minutes < 0 || hours < 0 || newInterval > 0)
            {
                if(newInterval > 0){
                    System.out.println("Setting new reminder interval...");
                    interval = newInterval;
                    newInterval = 0;
                    updateElements();
                    writeCurrentConfiguration();
                }else{
                    System.out.println("[" + new java.util.Date() + "] Be sure to take a break from screen time to avoid eye strain!");
                    if(trayIcon != null){
                        trayIcon.displayMessage(PROGRAM_TITLE + " Reminder", "Be sure to take a break from screen time to avoid eye strain!", java.awt.TrayIcon.MessageType.INFO);
                    }

                    if(remindWithDialog || trayIcon == null || !sysTraySupported){
                        if(!reminder.isShowing())
                            javafx.application.Platform.runLater(() -> {
                                reminder.showAndWait();
                        });

                        while(!reminder.isShowing()){
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                System.out.println("Sleep interrupted.");
                            }
                        }

                        while(reminder.isShowing()){
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                System.out.println("Sleep interrupted.");
                            }
                        }
                    }
                }
                now = Instant.now();
                nextReminder = now.plusMillis(interval);
                continue;
            }

            javafx.application.Platform.runLater(() -> {
                if(!disableReminders)
                    timeRemainingLbl.setText(getDurationAsString(dur, true));
            });

            try{
                if(!stage.isShowing()){
                    Thread.sleep(300);
                }else{
                    Thread.sleep(1);
                }
            }catch(InterruptedException intEx){
                System.out.println("Sleep interrupted.");
            }
        }
    }


    /**
     * Checks the current text and unit inputs to determine if a valid interval is specified.
     *
     * <p>
     *      Valid interval range: [1 minute, 10 hours]
     * </p>
     */
    public static void checkInput(){
        if(newIntervalField.getText().equals("")){
            // Text field is empty
            newIntervalField.pseudoClassStateChanged(INVALID_INPUT, false);
            setIntervalBt.setDisable(true);
        }else{
            try{
                long testInput = Long.parseLong(newIntervalField.getText());
                if(testInput > 0){
                    boolean validInput = false;
                    switch(FXCollections.observableArrayList(TIME_UNITS).indexOf(unitPicker.getValue())){
                        case MILLISECOND:
                            if(testInput >= 60000 && testInput <= 36000000)
                                validInput = true;
                            break;
                        case SECOND:
                            if(testInput >= 60 && testInput <= 36000)
                                validInput = true;
                            break;
                        case MINUTE:
                            if(testInput >= 1 && testInput <= 600)
                                validInput = true;
                            break;
                        case HOUR:
                            if(testInput >= 1 && testInput <= 10)
                                validInput = true;
                            break;
                        default:
                            break;
                    }
                    newIntervalField.pseudoClassStateChanged(INVALID_INPUT, !validInput);
                    setIntervalBt.setDisable(!validInput);
                }else{
                    throw new NumberFormatException();
                }
            }catch(NumberFormatException nfEx){
                newIntervalField.pseudoClassStateChanged(INVALID_INPUT, true);
                setIntervalBt.setDisable(true);
            }
        }
    }


    /**
     * Updates GUI elements based on various program settings.
     *
     */
    public static void updateElements(){

    	Instant nowStart = Instant.now();
    	final String intervalString = "Reminding every " + getDurationAsString(Duration.between(nowStart, nowStart.plusMillis(interval)), false);

    	// Set system tray elements
        if(trayIcon != null)
            javax.swing.SwingUtilities.invokeLater(() -> {
                if(!disableReminders){
                    trayIcon.setToolTip(PROGRAM_TITLE + "\n" + intervalString);
                    toggleRemindersTrayItem.setLabel("Disable reminders");
                }else{
                    trayIcon.setToolTip(PROGRAM_TITLE + "\n" + "Reminders are currently disabled.");
                    toggleRemindersTrayItem.setLabel("Enable reminders");
                }
        });

        // Update JavaFX elements
        javafx.application.Platform.runLater(() -> {
        	if(reminderBtPane != null){
	            if(reminderBtPane.getChildren().size() > 1)
	                reminderBtPane.getChildren().get(1).requestFocus();
	            else if(reminderBtPane.getChildren().size() > 0)
	                reminderBtPane.getChildren().get(0).requestFocus();
        	}

            if(trayIcon == null && fileMenu.getItems().contains(fileHide)){
                fileMenu.getItems().remove(fileHide);
            }else if(trayIcon != null && sysTraySupported && !fileMenu.getItems().contains(fileHide)){
                fileMenu.getItems().add(0, fileHide);
            }


            if(new File(CONFIG_FILE).exists()){
                editConfig.setDisable(false);
                reloadConfig.setDisable(false);
            }else{
                editConfig.setDisable(true);
                reloadConfig.setDisable(true);
            }


            if(useDarkTheme){
                toggleTheme.setText("Use light color scheme");

                if(reminder != null){
	                if(!((Stage)reminder.getDialogPane().getScene().getWindow()).getScene().getStylesheets().contains(DARK_STYLE_SHEET))
	                	((Stage)reminder.getDialogPane().getScene().getWindow()).getScene().getStylesheets().add(DARK_STYLE_SHEET);
                }

                if(!scene.getStylesheets().contains(DARK_STYLE_SHEET))
                    scene.getStylesheets().add(DARK_STYLE_SHEET);
            }else{
                toggleTheme.setText("Use dark color scheme");

                if(reminder != null){
	                if(((Stage)reminder.getDialogPane().getScene().getWindow()).getScene().getStylesheets().contains(DARK_STYLE_SHEET))
	                	((Stage)reminder.getDialogPane().getScene().getWindow()).getScene().getStylesheets().remove(DARK_STYLE_SHEET);
                }

                if(scene.getStylesheets().contains(DARK_STYLE_SHEET))
                    scene.getStylesheets().remove(DARK_STYLE_SHEET);
            }

            if(!disableReminders){
                nextReminderLbl.setText("Next reminder:");
                toggleReminders.setText("Disable reminders");
                timeRemainingLblTooltip.setText(intervalString);
                if(reminderBtPane != null && disableRemindersBt != null){
	                if(!reminderBtPane.getChildren().contains(disableRemindersBt))
	                    reminderBtPane.getChildren().add(0, disableRemindersBt);
                }
            }else{
                nextReminderLbl.setText("Reminders disabled.");
                toggleReminders.setText("Enable reminders");
                timeRemainingLblTooltip.setText("Reminders are currently disabled.\nYou can enable reminders from the Options menu.");
                timeRemainingLbl.setText("--:--:--.---");

                if(reminder != null){
                	((Stage)reminder.getDialogPane().getScene().getWindow()).close();
	                reminder.close();
                }
                if(reminderBtPane != null && disableRemindersBt != null){
	                if(reminderBtPane.getChildren().contains(disableRemindersBt))
	                    reminderBtPane.getChildren().remove(disableRemindersBt);
                }
            }

            checkInput();
        });
    }

    /**
     * Toggles reminder notifications and updates the GUI elements.
     *
     */
    public static void toggleReminders(){
        disableReminders = !disableReminders;

        if(trayIcon != null){
            if(disableReminders)
                trayIcon.displayMessage(PROGRAM_TITLE, "Reminders disabled", java.awt.TrayIcon.MessageType.INFO);
            else
                trayIcon.displayMessage(PROGRAM_TITLE, "Reminders enabled", java.awt.TrayIcon.MessageType.INFO);
        }

        updateElements();
    }


    /**
     * Creates a formatted String representing the given Duration
     *
     * @param dur
     *  The duration of time
     * @param useTimeStampFormat
     *  Determines whether to format the string as a timestamp.
     * <dl>
     * <dt>     Timestamp format:       </dt>
     * <dd>         00:00:00.000        </dd>
     * <dt>     Non-timestamp format:   </dt>
     * <dd>         00h 00m 00.000s     </dd>
     * </dl>
     *
     * @return
     *  formatted String representing the given Duration
     */
    public static String getDurationAsString(Duration dur, boolean useTimeStampFormat){
        long hours = dur.toHours();
        long minutes = dur.minusHours(hours).toMinutes();
        long seconds = dur.minusMinutes(minutes).getSeconds();
        long milliseconds = dur.minusMinutes(minutes).toMillis() - (seconds * 1000);

        String durString = "";
        if(hours < 10){
            durString += "0";
        }
        if(useTimeStampFormat)
            durString += hours + ":";
        else
            durString += hours + "h ";

        if(minutes < 10){
            durString += "0";
        }
        if(useTimeStampFormat)
            durString += minutes + ":";
        else
            durString  += minutes + "m ";

        if(seconds < 10){
            durString += "0";
        }
        if(useTimeStampFormat)
            durString += seconds + ".";
        else
            durString += seconds + ".";

        if(milliseconds < 100){
            durString += "0";
        }
        if(milliseconds < 10){
            durString += "0";
        }
        if(useTimeStampFormat)
            durString += "" + milliseconds;
        else
            durString += milliseconds + "s";

        return durString;
    }


    /**
     * Checks if the given line of text from a configuration file contains a
     *  possible setting key value pair.
     *
     * @param line
     *  A single line from a configuration file
     * @param key
     *  The configuration setting key that identifies a setting value
     *
     * @return true if the given string starts with the given preference key (case insensitive), false otherwise
     */
    public static boolean isConfigKeyValue(String line, String key){
        return (line.length() > key.length() && line.substring(0, key.length()).toUpperCase().equals(key.toUpperCase()));
    }

    /**
     * Gets a setting value from the given line of text from a configuration file.
     *
     * @param line
     *  A single line from a configuration file
     * @param key
     *  The configuration setting key that identifies a setting value
     *
     * @return
     *  the substring of the given line that follows the given key string
     */
    public static String getConfigKeyValue(String line, String key){
        return line.substring(key.length()).trim();
    }


    /**
     * Writes the current configuration to the program configuration file.
     *
     * @return
     *  true if successful, false otherwise
     */
    public static boolean writeCurrentConfiguration(){

        // Build contents of config file
        String configText = String.format("%n");
        configText += STARTUP_HIDE_KEY;
        if(hideOnStartup)
            configText += "1";
        else
            configText += "0";

        configText += String.format("%n%n") + REMIND_WITH_DIALOG_KEY;
        if(remindWithDialog)
            configText += "1";
        else
            configText += "0";

        configText += String.format("%n%n") + INTERVAL_KEY + interval;

        configText += String.format("%n%n") + DARK_THEME_KEY;
        if(useDarkTheme)
            configText += "1";
        else
            configText += "0";

        configText += String.format("%n%n");

        File configFile = new File(CONFIG_FILE);

        if(configFile.exists())
            configFile.delete();

        try {
            configFile.createNewFile();
            writeTextFile(CONFIG_FILE, configText, true);
        } catch (IOException e) {
            // Unable to create new configuration file
            System.out.println("ERROR: Unable to write configuration file.");
            return false;
        }

        System.out.println("Settings saved.");
        return true;
    }


    /**
     * Reads a text file and returns the contents as a String.
     *
     * @param filePath
     *  the path of the file to be read, including the filename
     * @throws FileNotFoundException
     *  If the specified file doesn't exist
     *
     * @return the contents of the specified file as a String
     */
    public static String readTextFile(String filePath) throws FileNotFoundException{
        File readFile = new File(filePath);
        //Initialize Scanner for reading file
        Scanner fileReader = new Scanner(readFile);
        String text = null;

        while(fileReader.hasNextLine()){
            if(text == null){
                //Initialize return String
                text = "";
            }else{
                //Add new linebreak
                text += String.format("%n");
            }

            //Add line to text
            text += fileReader.nextLine();
        }

        fileReader.close();

        // If text is still null, return empty string
        if(text == null)
            text = "";
        return text;
    }



    /**
     * Writes a string to a text file
     *
     * @param filePath
     *  the path of the file to be read, including the filename
     * @param text
     *  the String to be written to the file; can be more than one line.
     * @param overwrite
     *  determines whether the user wants to overwrite the write file if it
     *  already exists. If true, pre-existing file will be overwritten
     * @throws IIOException
     *  if the write file already exists and the user allowed overwriting, but
     *  the file could not be overwritten
     * @throws AccessDeniedException
     *  if the write file already exists but the user didn't allow overwriting
     * @throws IOException
     *  if an error occurs initializing the BufferedWriter
     */
    public static void writeTextFile(String filePath, String text, boolean overwrite)
        throws IIOException, IOException, AccessDeniedException{

        //The file to be written
        File writeFile = new File(filePath);
        if(writeFile.exists() && overwrite){
            //If file exists, try to delete it
            if(!writeFile.delete()){
                //If file cannot be deleted, throw OIOException
                throw new IIOException("Could not delete pre-existing file: " + filePath);
            }
        }else if(writeFile.exists() && !overwrite){
            //If file exists but is not allowed to be overwritten, throw AccessDeniedException
            throw new AccessDeniedException(writeFile.getPath());
        }

        //Initialize BufferedWriter to write string to file
        BufferedWriter fileWriter = new BufferedWriter(new FileWriter(writeFile));

        //Write the file
        fileWriter.write(text, 0, text.length());

        fileWriter.close();
    }
}
