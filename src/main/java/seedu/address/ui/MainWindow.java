package seedu.address.ui;

import java.io.File;
import java.util.Set;
import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.Logic;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";
    private static final Set<String> CONFIRM_WORDS = Set.of("y", "yes");
    private static final String IMPORT_DATA_TITLE = "Import Data";
    private static final String EXPORT_DATA_TITLE = "Export Data";
    private static final Set<FileChooser.ExtensionFilter> ACCEPTED_FILE_EXTENSIONS = Set.of(
            new FileChooser.ExtensionFilter("Data Files", "*.json", "*.csv")
    );

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;
    private boolean isPrompt = false; // Tracks if this MainWindow is currently waiting for confirmation from user
    private CommandResult lastCommandResult = null; // Tracks the most recent CommandResult

    // Independent Ui parts residing in this Ui container
    private CommandBox commandBox;
    private PersonListPanel personListPanel;
    private RentalInformationListPanel rentalInformationListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;
    private FileChooser fileChooser;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane rentalInformationListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage} and {@code Logic}.
     */
    public MainWindow(Stage primaryStage, Logic logic) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();

        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(ACCEPTED_FILE_EXTENSIONS);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in later version of SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField, and
         * ResultDisplay contains a TextArea, thus some accelerators (e.g F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add following event filter to capture such key events and open
         * help window purposely so to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        personListPanel = new PersonListPanel(logic.getFilteredPersonList());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        rentalInformationListPanel = new RentalInformationListPanel(logic.getVisibleRentalInformationList(),
                logic.getVisibleClient());
        rentalInformationListPanelPlaceholder.getChildren().add(rentalInformationListPanel.getRoot());

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        StatusBarFooter statusBarFooter = new StatusBarFooter(logic.getAddressBookFilePath());
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        helpWindow.hide();
        primaryStage.hide();
    }

    private File importFile() {
        fileChooser.setTitle(IMPORT_DATA_TITLE);
        return fileChooser.showOpenDialog(getPrimaryStage());
    }

    private File exportFile() {
        fileChooser.setTitle(EXPORT_DATA_TITLE);
        return fileChooser.showSaveDialog(getPrimaryStage());
    }

    // TODO: the methods below are temporary to test the FileChooser
    @FXML
    private void handleImport() {
        importFile();
    }
    @FXML
    private void handleExport() {
        exportFile();
    }

    public PersonListPanel getPersonListPanel() {
        return personListPanel;
    }

    /**
     * Executes the command and returns the result.
     *
     * @see seedu.address.logic.Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult;
            if (!isPrompt) {
                commandResult = logic.execute(commandText);
            } else {
                commandResult = checkConfirmation(commandText);
            }
            lastCommandResult = commandResult;

            switch (commandResult.getType()) {
            case PROMPT:
                logger.info("Prompt: " + commandResult.getFeedbackToUser());
                isPrompt = true;
                break;
            default:
                logger.info("Result: " + commandResult.getFeedbackToUser());
                break;
            }

            resultDisplay.setFeedbackToUser(commandResult.getFeedbackToUser());
            commandBox.setFeedbackToUser(commandResult.getHistory());

            switch (commandResult.getType()) {
            case SHOW_HELP:
                handleHelp();
                break;
            case EXIT:
                handleExit();
                break;
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            throw e;
        }
    }

    private CommandResult checkConfirmation(String userInput) throws CommandException {
        isPrompt = false;

        if (!isConfirmation(userInput)) {
            return new CommandResult("Command cancelled");
        }
        return lastCommandResult.confirmPrompt();
    }

    /**
     * Checks if the given user input corresponds to user confirming a prompt.
     */
    private boolean isConfirmation(String userInput) {
        return CONFIRM_WORDS.contains(userInput.trim().toLowerCase());
    }
}
