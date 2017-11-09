// Author: Sean Pesce

package restyoureyes;

import static restyoureyes.Constants.*;

import java.awt.Toolkit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.AccessDeniedException;
import java.time.Duration;
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
 * screen time to avoid eye strain. A reminder is sent every time the configurable
 * interval period expires.
 *
 * @author Sean Pesce
 */
public class RestYourEyes extends Application {

	// Configurable settings
	private static Client client;
	private static Prefs prefs;

	private static double windowWidth = DEFAULT_WINDOW_WIDTH;
	private static double windowHeight = DEFAULT_WINDOW_HEIGHT;


	// User Interface nodes
	private static Stage stage;
	private static Scene scene;
	private static Group group;
	private static VBox primaryVBox;
	private static MenuBar fileToolbar;
	private static Menu fileMenu;
	private static MenuItem fileHide;
	private static MenuItem fileExit;
	private static Menu optionsMenu;
	private static MenuItem toggleReminders;
	private static MenuItem toggleAggressive;
	private static MenuItem toggleTheme;
	private static VBox nextReminderVBox;
	private static Label nextReminderLbl;
	private static Label timeRemainingLbl;
	private static Tooltip timeRemainingLblTooltip;
	private static HBox setIntervalHBox;
	private static TextField newIntervalField;
	private static ComboBox<String> unitPicker;
	private static Button setIntervalBt;

	private static Alert reminder;
//	private static Button disableRemindersBt;
//	private static HBox reminderBtPane;

	private static java.awt.TrayIcon trayIcon;
	private static java.awt.MenuItem toggleRemindersTrayItem;

	// Variables
	// set runLoop to false to signal timerloop to terminate
	private static volatile boolean runLoop = true;
	// disable reminders to true to make timer display elements dormant
	private static volatile boolean disableReminders = false;
	private static boolean sysTraySupported = false;
	private static int port;


	/**
	 * @param args The list of command line arguments passed to the program.
	 */
	public static void main(String[] args) {
		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException nex) {
				port = DEFAULT_SERVER_PORT;
			}
		} else {
			port = DEFAULT_SERVER_PORT;
		}
		launch(args);
	}

	/**
	 * Connect a client to the host application before the gui is created.
	 *
	 * @throws Exception
	 */
	@Override
	public void init() throws Exception {
		client = new Client(port);
		prefs = client.getPrefs();
		client.setCloser((code, reason, remote) -> {
			System.out.println("Close " + code + ":" + reason + ":" + remote);
			Platform.exit();
		});
		client.setMessager((message) -> {
			System.out.println("[" + new java.util.Date() + "] " + message);
			if (prefs.isAggressiveReminders()) {
				if (!reminder.isShowing()) {
					Platform.runLater(() -> reminder.showAndWait());
				}
			}
		});
	}

	/**
	 * Gracefully disconnect the client before closing the application.
	 *
	 * @throws Exception
	 */
	@Override
	public void stop() throws Exception {
		runLoop = false;
		client.close();
	}


	/**
	 * The main entry point for all JavaFX applications. The start method is called
	 * after the init method has returned, and after the system is ready for the
	 * application to begin running.
	 * <p>
	 * <p>
	 * NOTE: This method is called on the JavaFX Application Thread.
	 * </p>
	 *
	 * @param newStage The stage supplied for the primary window of the program.
	 * @throws java.lang.Exception
	 */
	@Override
	public void start(Stage newStage) throws Exception {
		try {
			stage = newStage;
			initWindow();
//			initSystemTray();
			initEventHandlers();

			Thread timerThread = new Thread(() -> timerLoop());
			timerThread.start();

			stage.show();
			updateElements();
		} catch (Exception ex) {
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
	 * Initializes the main program window.
	 */
	private static void initWindow() {
		System.out.println("Initializing main window...");

		// Exit when all stages are closed
		Platform.setImplicitExit(true);

		stage.setWidth(windowWidth);
		stage.setHeight(windowHeight);

		group = new Group();
		scene = new Scene(group);
		scene.getStylesheets().add(DEFAULT_STYLE_SHEET);
		if (prefs.isDarkTheme()) {
			scene.getStylesheets().add(DARK_STYLE_SHEET);
		}

		fileToolbar = new MenuBar();
		fileMenu = new Menu("File");
		fileHide = new MenuItem("Hide");
		fileExit = new MenuItem("Exit");
		fileMenu.getItems().addAll(fileHide, fileExit);

		optionsMenu = new Menu("Options");
		toggleAggressive = new MenuItem("Enable aggressive behavior");
		if (prefs.isAggressiveReminders()) {
			toggleAggressive.setText("Disable aggressive behavior");
		}
		toggleReminders = new MenuItem("Disable reminders");
		if (disableReminders) {
			toggleReminders.setText("Enable reminders");
		}
		toggleTheme = new MenuItem("Use dark color scheme");
		if (prefs.isDarkTheme()) {
			toggleTheme.setText("Use light color scheme");
		}

		optionsMenu.getItems().addAll(toggleAggressive, toggleReminders, toggleTheme);
		fileToolbar.getMenus().addAll(fileMenu, optionsMenu);


		nextReminderLbl = new Label("Next reminder:");
		nextReminderLbl.setStyle("-fx-font-size: 10px;");
		timeRemainingLbl = new Label("--:--:--.---");
		timeRemainingLbl.getStyleClass().add("timer");
		timeRemainingLbl.setPadding(new Insets(3.0, 10.0, 3.0, 10.0));
		timeRemainingLblTooltip = new Tooltip("Reminders are currently disabled.\nYou can enable reminders from the " +
				"Options menu.");
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
		try {
			stage.getIcons().add(new Image(ClassLoader.getSystemResourceAsStream(PROGRAM_ICON)));
		} catch (Exception ex) {
			System.out.println("ERROR: Unable to load window icon");
		}

		primaryVBox.getStyleClass().add("main_window");

		initReminderWindow();

		timeRemainingLbl.requestFocus();

		primaryVBox.setPadding(new Insets(fileToolbar.getHeight(), 10.0, 10.0, 10.0));
	}

	/**
	 * Initializes the reminder dialog window.
	 */
	private static void initReminderWindow() {
		reminder = new Alert(Alert.AlertType.NONE, "Be sure to take a break from screen time to avoid eye strain!");
		HBox reminderBtPane = new HBox();
		Button disableRemindersBt = new Button("Disable reminders");

		reminder.setResizable(false);
		Stage remStage = (Stage) reminder.getDialogPane().getScene().getWindow();
		remStage.getScene().getStylesheets().add(DEFAULT_STYLE_SHEET);
		if (prefs.isDarkTheme()) {
			remStage.getScene().getStylesheets().add(DARK_STYLE_SHEET);
		}

		try {
			remStage.getIcons().add(new Image(ClassLoader.getSystemResourceAsStream(PROGRAM_ICON)));
		} catch (Exception ex) {
			System.out.println("ERROR: Unable to load reminder window icon");
		}
		remStage.setTitle("Rest your eyes!");
		DialogPane dPane = reminder.getDialogPane();
		dPane.setPrefWidth(350);
		dPane.setPrefHeight(130);
		dPane.setMaxHeight(131);
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
//		disableRemindersBt.setOnAction(e -> {
//			if (!disableReminders) {
//				toggleReminders();
//			}
//		});

		remStage.setOnCloseRequest(e -> {
			//remStage.close();
			//reminder.close();
		});

		remStage.setAlwaysOnTop(true);
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
				if (trayIcon != null) {
					java.awt.SystemTray.getSystemTray().remove(trayIcon);
				}
				String iconRelPath = "style/" + PROGRAM_ICON_WHITE;
				if (!new File(iconRelPath).exists()) {
					iconRelPath = PROGRAM_ICON_WHITE;
				}
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

				// Convention for tray icons seems to be to set the default icon for opening the application stage in
				// a bold font.
				java.awt.Font defaultFont = java.awt.Font.decode(null);
				java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
				openItem.setFont(boldFont);

				// To truly exit the application, the user must go to the system tray icon (or file menu)
				// and select the "exit" option; this will shutdown JavaFX and remove the
				// tray icon (removing the tray icon will also shut down AWT).
				java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
				exitItem.addActionListener(event -> {
					javafx.application.Platform.runLater(() -> {
						if (reminder.isShowing()) {
							((Stage) reminder.getDialogPane().getScene().getWindow()).close();
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
	 * Initializes the handler functions that respond to user interaction with the GUI.
	 */
	private static void initEventHandlers() {

		fileToolbar.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldHeight,
												  Number newHeight) -> {
			primaryVBox.setPadding(new Insets(fileToolbar.getHeight(), 10.0, 10.0, 10.0));
		});

		scene.widthProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldWidth, Number
				newWidth) -> {
			primaryVBox.setPrefWidth(scene.getWidth());
			fileToolbar.setPrefWidth(scene.getWidth());
		});
		primaryVBox.setPrefWidth(scene.getWidth());
		fileToolbar.setPrefWidth(scene.getWidth());


		scene.heightProperty().addListener((ObservableValue<? extends Number> observableValue, Number oldHeight,
											Number newHeight) -> {
			primaryVBox.setPrefHeight(scene.getHeight());
		});
		primaryVBox.setPrefHeight(scene.getHeight());


		scene.setOnMouseClicked((MouseEvent mouseEvent) -> {
			if (mouseEvent.getButton().equals(MouseButton.PRIMARY) && mouseEvent.getClickCount() == 2) {
				timeRemainingLbl.requestFocus();
			}
		});


		newIntervalField.textProperty().addListener(
				(ObservableValue<? extends String> observableValue, String oldText, String newText) -> checkInput()
		);
		unitPicker.setOnAction(e -> checkInput());


		setIntervalBt.setOnAction(e -> {
			try {
				String inputString = newIntervalField.getText();
				newIntervalField.clear();
				timeRemainingLbl.requestFocus();
				long newInt = Long.parseLong(inputString);
				switch (FXCollections.observableArrayList(TIME_UNITS).indexOf(unitPicker.getValue())) {
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
				if (newInt < 60000 || newInt > 36000000) {
					throw new NumberFormatException();
				}

				prefs.setInterval(newInt);
				client.setPrefs(prefs);

			} catch (NumberFormatException nfEx) {
				System.out.println("ERROR: Invalid interval specified.");
			} catch (IOException ex) {
				ex.printStackTrace();
				Platform.exit();
			}
		});


		toggleAggressive.setOnAction(e -> toggleAggressive());
		toggleReminders.setOnAction(e -> toggleReminders());


		toggleTheme.setOnAction(e -> {
			prefs.setDarkTheme(!prefs.isDarkTheme());
			updateElements();
			try {
				client.setPrefs(prefs);
			} catch (IOException ex) {
				ex.printStackTrace();
				Platform.exit();
			}
		});

		stage.setOnCloseRequest(e -> Platform.exit());

		fileHide.setOnAction(e -> Platform.exit());

		fileExit.setOnAction(e -> {
			// TODO close signal to host application
			Platform.exit();
		});
	}

	/**
	 * Continuously runs a loop in a separate thread from the rest of the program
	 * and performs the following functions:
	 * <p>
	 * <ul>
	 * <li>
	 * Updates the timer display.
	 * <\li>
	 * <li>
	 * Checks if the reminder timer has expired, at which point a reminder
	 * notification is sent to the user and the timer is restarted.
	 * <\li>
	 * </ul>
	 */
	private static void timerLoop() {

		System.out.println("Starting timer...");

		long remaining;

		updateElements();

		while (runLoop) {
			while (disableReminders) {
				updateElements();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					System.out.println("Sleep interrupted.");
				}
			}

			try {
				remaining = client.getRemaining();
			} catch (Exception ex) {
				ex.printStackTrace();
				Platform.exit();
				return;
			}

			// compiler wants us to pass a constant into lambda expr, otherwise
			// error: local variables referenced from a lambda expression must be final or effectively final
			final long remainingSnapshot = remaining;
			javafx.application.Platform.runLater(() -> {
				if (!disableReminders) {
					timeRemainingLbl.setText(getDurationAsString(Duration.ofMillis(remainingSnapshot), true));
				}
			});

			try {
				if (!stage.isShowing()) {
					Thread.sleep(300);
				} else {
					Thread.sleep(1);
				}
			} catch (InterruptedException intEx) {
				System.out.println("Sleep interrupted.");
			}
		}
	}


	/**
	 * Checks the current text and unit inputs to determine if a valid interval is specified.
	 * <p>
	 * <p>
	 * Valid interval range: [1 minute, 10 hours]
	 * </p>
	 */
	private static void checkInput() {
		if (newIntervalField.getText().equals("")) {
			// Text field is empty
			newIntervalField.pseudoClassStateChanged(INVALID_INPUT, false);
			setIntervalBt.setDisable(true);
		} else {
			try {
				long testInput = Long.parseLong(newIntervalField.getText());
				if (testInput > 0) {
					boolean validInput = false;
					switch (FXCollections.observableArrayList(TIME_UNITS).indexOf(unitPicker.getValue())) {
						case MILLISECOND:
							if (testInput >= 60000 && testInput <= 36000000) {
								validInput = true;
							}
							break;
						case SECOND:
							if (testInput >= 60 && testInput <= 36000) {
								validInput = true;
							}
							break;
						case MINUTE:
							if (testInput >= 1 && testInput <= 600) {
								validInput = true;
							}
							break;
						case HOUR:
							if (testInput >= 1 && testInput <= 10) {
								validInput = true;
							}
							break;
						default:
							break;
					}
					newIntervalField.pseudoClassStateChanged(INVALID_INPUT, !validInput);
					setIntervalBt.setDisable(!validInput);
				} else {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException nfEx) {
				newIntervalField.pseudoClassStateChanged(INVALID_INPUT, true);
				setIntervalBt.setDisable(true);
			}
		}
	}


	/**
	 * Updates GUI elements based on various program settings.
	 */
	private static void updateElements() {
		final String intervalString = "Reminding every " + getDurationAsString(Duration.ofMillis(prefs.getInterval()),
				false);
		// Set system tray elements
		if (trayIcon != null) {
			javax.swing.SwingUtilities.invokeLater(() -> {
				if (!disableReminders) {
					trayIcon.setToolTip(PROGRAM_TITLE + "\n" + intervalString);
					toggleRemindersTrayItem.setLabel("Disable reminders");
				} else {
					trayIcon.setToolTip(PROGRAM_TITLE + "\n" + "Reminders are currently disabled.");
					toggleRemindersTrayItem.setLabel("Enable reminders");
				}
			});
		}

		// Update JavaFX elements
		javafx.application.Platform.runLater(() -> {
//			if (reminderBtPane != null) {
//				if (reminderBtPane.getChildren().size() > 1) {
//					reminderBtPane.getChildren().get(1).requestFocus();
//				} else if (reminderBtPane.getChildren().size() > 0) {
//					reminderBtPane.getChildren().get(0).requestFocus();
//				}
//			}

//			if (trayIcon == null && fileMenu.getItems().contains(fileHide)) {
//				fileMenu.getItems().remove(fileHide);
//			} else if (trayIcon != null && sysTraySupported && !fileMenu.getItems().contains(fileHide)) {
//				fileMenu.getItems().add(0, fileHide);
//			}


			if (prefs.isDarkTheme()) {
				toggleTheme.setText("Use light color scheme");

				if (reminder != null) {
					if (!((Stage) reminder.getDialogPane().getScene().getWindow()).getScene().getStylesheets()
							.contains(DARK_STYLE_SHEET)) {
						((Stage) reminder.getDialogPane().getScene().getWindow()).getScene().getStylesheets().add
								(DARK_STYLE_SHEET);
					}
				}

				if (!scene.getStylesheets().contains(DARK_STYLE_SHEET)) {
					scene.getStylesheets().add(DARK_STYLE_SHEET);
				}
			} else {
				toggleTheme.setText("Use dark color scheme");

				if (reminder != null) {
					if (((Stage) reminder.getDialogPane().getScene().getWindow()).getScene().getStylesheets().contains
							(DARK_STYLE_SHEET)) {
						((Stage) reminder.getDialogPane().getScene().getWindow()).getScene().getStylesheets().remove
								(DARK_STYLE_SHEET);
					}
				}

				if (scene.getStylesheets().contains(DARK_STYLE_SHEET)) {
					scene.getStylesheets().remove(DARK_STYLE_SHEET);
				}
			}

			if (prefs.isAggressiveReminders()) {
				toggleAggressive.setText("Disable aggressive behavior");
			} else {
				toggleAggressive.setText("Enable aggressive behavior");
			}

			if (disableReminders) {
				nextReminderLbl.setText("Reminders disabled.");
				toggleReminders.setText("Enable reminders");
				timeRemainingLblTooltip.setText("Reminders are currently disabled.\nYou can enable reminders from the" +
						" " +
						"Options menu.");
				timeRemainingLbl.setText("--:--:--.---");

				if (reminder != null) {
					((Stage) reminder.getDialogPane().getScene().getWindow()).close();
					reminder.close();
				}
//				if (reminderBtPane != null && disableRemindersBt != null) {
//					if (reminderBtPane.getChildren().contains(disableRemindersBt)) {
//						reminderBtPane.getChildren().remove(disableRemindersBt);
//					}
//				}
			} else {
				nextReminderLbl.setText("Next reminder:");
				toggleReminders.setText("Disable reminders");
				timeRemainingLblTooltip.setText(intervalString);
//				if (reminderBtPane != null && disableRemindersBt != null) {
//					if (!reminderBtPane.getChildren().contains(disableRemindersBt)) {
//						reminderBtPane.getChildren().add(0, disableRemindersBt);
//					}
//				}
			}

			checkInput();
		});
	}

	/**
	 * Toggles whether the gui opens a popup window for a reminder.
	 */
	private static void toggleAggressive() {
		prefs.setAggressiveReminders(!prefs.isAggressiveReminders());
		try {
			client.setPrefs(prefs);
		} catch (IOException iex) {
			iex.printStackTrace();
			Platform.exit();
		}
		updateElements();
	}

	/**
	 * Toggles whether reminders happen at all.  Does not pause or reset the actual timer.
	 */
	private static void toggleReminders() {
		disableReminders = !disableReminders;
		try {
			client.toggleReminders();
		} catch (IOException iex) {
			iex.printStackTrace();
			Platform.exit();
		}
		updateElements();
	}


	/**
	 * Creates a formatted String representing the given Duration
	 *
	 * @param dur                The duration of time
	 * @param useTimeStampFormat Determines whether to format the string as a timestamp.
	 *                           <dl>
	 *                           <dt>     Timestamp format:       </dt>
	 *                           <dd>         00:00:00.000        </dd>
	 *                           <dt>     Non-timestamp format:   </dt>
	 *                           <dd>         00h 00m 00.000s     </dd>
	 *                           </dl>
	 * @return formatted String representing the given Duration
	 */
	private static String getDurationAsString(Duration dur, boolean useTimeStampFormat) {
		long hours = dur.toHours();
		long minutes = dur.minusHours(hours).toMinutes();
		long seconds = dur.minusMinutes(minutes).getSeconds();
		long milliseconds = dur.minusMinutes(minutes).toMillis() - (seconds * 1000);

		String durString = "";
		if (hours < 10) {
			durString += "0";
		}
		if (useTimeStampFormat) {
			durString += hours + ":";
		} else {
			durString += hours + "h ";
		}

		if (minutes < 10) {
			durString += "0";
		}
		if (useTimeStampFormat) {
			durString += minutes + ":";
		} else {
			durString += minutes + "m ";
		}

		if (seconds < 10) {
			durString += "0";
		}
		if (useTimeStampFormat) {
			durString += seconds + ".";
		} else {
			durString += seconds + ".";
		}

		if (milliseconds < 100) {
			durString += "0";
		}
		if (milliseconds < 10) {
			durString += "0";
		}
		if (useTimeStampFormat) {
			durString += "" + milliseconds;
		} else {
			durString += milliseconds + "s";
		}

		return durString;
	}


	/**
	 * Writes a string to a text file
	 *
	 * @param filePath  the path of the file to be read, including the filename
	 * @param text      the String to be written to the file; can be more than one line.
	 * @param overwrite determines whether the user wants to overwrite the write file if it
	 *                  already exists. If true, pre-existing file will be overwritten
	 * @throws IIOException          if the write file already exists and the user allowed overwriting, but
	 *                               the file could not be overwritten
	 * @throws AccessDeniedException if the write file already exists but the user didn't allow overwriting
	 * @throws IOException           if an error occurs initializing the BufferedWriter
	 */
	private static void writeTextFile(String filePath, String text, boolean overwrite)
			throws IIOException, IOException, AccessDeniedException {

		//The file to be written
		File writeFile = new File(filePath);
		if (writeFile.exists() && overwrite) {
			//If file exists, try to delete it
			if (!writeFile.delete()) {
				//If file cannot be deleted, throw OIOException
				throw new IIOException("Could not delete pre-existing file: " + filePath);
			}
		} else if (writeFile.exists() && !overwrite) {
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
