package dissertation.gui.controllers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import dissertation.Compiler;
import dissertation.Main;
import dissertation.Word;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Pair;

public class MainController {
    private static final Preferences PREFS = Preferences.userNodeForPackage(MainController.class);
    private static final String KEY_LAST_FILE_PATH = "lastProgramFilePath";
    private static String loadedProgram = "";
    private static File loadedFile = null;
    private Main machine = new Main();
    private boolean manualPipeline;
    private boolean modeChosen = false;
    private static boolean macroScene = false;
    private static final String IMPORTS = "\\bimport\\b";
    private static final String OPCODES   = "\\b(?:load|store|add|addf|and|or|xor|jump|halt|move|loadi|out|rotate)\\b";
    private static final String REGISTER  = "\\breg(?:[0-9]|1[0-5])\\b";
    private static final String NUMBER    = "\\b[0-9]+\\b";
    private static final String LABEL    = "\\b[A-Za-z_]\\w*:";

    private static final String COMMENT   = "//[^\\n]*";
    private static final Pattern SYNTAX_PATTERN = Pattern.compile(
        "(?<IMP>"  + IMPORTS + ")"
        + "|(?<OP>"  + OPCODES     + ")"
        + "|(?<LBL>" + LABEL      + ")"
        + "|(?<REG>" + REGISTER   + ")"
        + "|(?<NUM>" + NUMBER     + ")"
        + "|(?<COMM>"+ COMMENT    + ")",
        Pattern.CASE_INSENSITIVE
    );

    private enum PipelineStage { WB, MEM, EX, ID, IF }
    private PipelineStage nextStage;

    @FXML
    private VBox mainContainer;

    @FXML
    private Label titleLabel;

    @FXML
    private Button actionButton;

    @FXML
    private Button viewInstructionsButton;    

    @FXML
    private Button loadProgramButton;

    @FXML
    private Button newProgramButton;

    @FXML
    private Button resetProgramButton;

    @FXML
    private Button createMacroButton;

    @FXML
    private Button viewMacrosButton;

    @FXML
    private Button exitButton;

    @FXML 
    private TextFlow outputText;

    public void initialize() {
        //Try to load the last program file using preferences.
        String lastPath = PREFS.get(KEY_LAST_FILE_PATH, null);
        if (lastPath != null) {
            File file = new File(lastPath);
            if (file.exists()) {
                loadedFile = file;
                try (BufferedReader reader = new BufferedReader(new java.io.FileReader(loadedFile))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    loadedProgram = sb.toString();
                } catch (IOException ex) {
                    ex.printStackTrace();

                    clearAndAddText("Error loading the last used program.");
                }
            } else {
            }
        } else {
        }
        
        titleLabel.setText("Arya's Brookshear Machine");
        actionButton.setOnAction(event -> onActionButton());

        viewInstructionsButton.setOnAction(event-> handleViewInstructionsButtonClick());

        newProgramButton.setOnAction(event -> handleNewProgramButtonClick());
        loadProgramButton.setOnAction(event -> handleLoadProgramButtonClick());

        createMacroButton.setOnAction(event -> handleCreateMacroButtonClick());

        viewMacrosButton.setOnAction(event -> {handleViewMacrosButtonClick();});

        if (!machine.getStarted()){
            resetProgramButton.setDisable(true);
        }
        resetProgramButton.setOnAction(event -> onResetProgramButton());

        exitButton.setOnAction(event -> handleExitButtonClick());
    }

    private void handleExitButtonClick() {
        //put an alert to confirm that they have saved their program before exiting
        javafx.scene.control.Alert confirmAlert = 
            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Exit");
        confirmAlert.setHeaderText("Exit Program");
        confirmAlert.setContentText("Are you sure you want to exit? Any unsaved changes will be lost.");
        confirmAlert.showAndWait();
        if (confirmAlert.getResult() == javafx.scene.control.ButtonType.OK) {
            System.exit(0);
        }
    }

    private void onResetProgramButton() {
        Main.resetProgram();
        resetProgramButton.setDisable(true);
        actionButton.setDisable(false);
        modeChosen = false;
        outputText.getChildren().clear();
    }

    @SuppressWarnings("static-access")
    private void stepOneInstruction() {
        if (machine.isHalted()) {
            return;
        }
        Main.tickprogram();
        dumpBuses();
    }

    private void dumpBuses() {
        @SuppressWarnings("static-access")
        var buses = machine.getBusValues();
        StringBuilder sb = new StringBuilder();
        for (var e : buses.entrySet()) {
            String name = e.getKey();
            Object val  = e.getValue();
            String s;
            if (val instanceof dissertation.Byte b) {
                s = String.format("0x%02X", b.byteToHex());
            } else if (val instanceof Word w) {
                s = String.format("0x%04X", w.wordToHex());
            } else {
                s = val.toString();
            }
            sb.append(name).append("=").append(s).append("  ");
        }
    }

    

    @SuppressWarnings("static-access")
    private void stepOneStage() {
        if (machine.isHalted()) {
            return; 
        }
        switch (nextStage) {
          case WB:
            Main.pipelineRun(Main.PipelineStage.WB);
            clearAndAddText("→ WriteBack\n");
            nextStage = PipelineStage.MEM; break;
          case MEM:
            Main.pipelineRun(Main.PipelineStage.MEM);
            clearAndAddText("→ Memory\n");
            nextStage = PipelineStage.EX;  break;
          case EX:
            Main.pipelineRun(Main.PipelineStage.EX);
            clearAndAddText("→ Decode\n");
            nextStage = PipelineStage.ID;  break;
          case ID:
            Main.pipelineRun(Main.PipelineStage.ID);
            clearAndAddText("→ Fetch\n\n");
            nextStage = PipelineStage.IF;  break;
          case IF:
            Main.pipelineRun(Main.PipelineStage.IF);
            clearAndAddText("→ Commit\n\n");
            nextStage = PipelineStage.WB;  break;
        }
        dumpBuses();
    }

    private void onActionButton() {
        if (!modeChosen) {
            chooseModeAndInitialize();
            return; 
        }

        // from now on, each click just steps
        if (manualPipeline) {
            stepOneStage();
        } else {
            stepOneInstruction();
        }
    }

    private void chooseModeAndInitialize() {
        //Run Full vs Step-by-Step
        ChoiceDialog<String> m1 = new ChoiceDialog<>("Run Full", "Run Full", "Step-by-Step");
        m1.setTitle("Execution Mode");
        m1.setHeaderText("Choose how to run the program");
        Optional<String> c1 = m1.showAndWait();
        if (c1.isEmpty()) return;
        outputText.getChildren().clear();

        String mode = c1.get();
        Main.loadProgram(loadedProgram);

        if (mode.equals("Run Full")) {
            Main.loadProgram(loadedProgram);
            Main.startProgram();
            Main.tickprogram();
            actionButton.setDisable(true);
            resetProgramButton.setDisable(false);
        
            //display the outBus-change log:
            var changes = Main.getOutBusChanges();
            if (changes.isEmpty()) {
                clearAndAddText("No outBus changes during full run.\n");
            } else {
                for (Main.OutBusChange c : changes) {
                    String line = String.format("Cycle %d: outBus = 0x%02X%n",
                                                c.cycle(),
                                                c.value().wordToHex());
                    outputText.getChildren().add(new Text(line));
                }
            }
            return;
        }
        

        ChoiceDialog<String> m2 = new ChoiceDialog<>("Manual", "Manual", "Automatic");
        m2.setTitle("Pipeline Mode");
        m2.setHeaderText("Stepping Mode");
        Optional<String> c2 = m2.showAndWait();
        if (c2.isEmpty()) return;

        if (c2.get().equals("Automatic")) {
            manualPipeline = false;
            modeChosen = true;
            actionButton.setDisable(true);
            resetProgramButton.setDisable(false);
            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dissertation/gui/PipelineView.fxml")
                );
                Parent root = loader.load();
                PipelineViewController pvc = loader.getController();
                pvc.init(false);    // false = “automatic” mode
                Stage win = new Stage();
                win.setTitle("Pipeline Viewer & Stepper");
                win.setScene(new Scene(root));
                win.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR,
                    "Failed to load pipeline viewer: " + ex.getMessage()
                ).showAndWait();
            }
        } else {
            manualPipeline = (c2.get().equals("Manual"));
            modeChosen    = true;
            actionButton.setDisable(true);
            resetProgramButton.setDisable(false);

            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dissertation/gui/PipelineView.fxml")
                );
                Parent root = loader.load();
                PipelineViewController pvc = loader.getController();
                pvc.init(manualPipeline);

                Stage win = new Stage();
                win.setTitle("Pipeline Viewer");
                win.setScene(new Scene(root));
                win.show();
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR,
                    "Failed to load pipeline viewer: " + e.getMessage()
                ).showAndWait();
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleViewInstructionsButtonClick() {
        @SuppressWarnings("unchecked")
        ObservableList<ObservableList<String>> data = FXCollections.observableArrayList(
            FXCollections.observableArrayList("load", "regX memory-location",
                "LOAD the register X with the bit pattern found in the memory cell whose address is location.\n" +
                "Example: load reg4 50 would cause the contents of the memory cell located at address 50 to be placed in register 4."),
            FXCollections.observableArrayList("loadi", "regX value",
                "LOAD the register X with the number value.\n" +
                "Example: loadi reg1 20 would cause the value 20 to be placed in register 1."),
            FXCollections.observableArrayList("store", "regX memory-location",
                "STORE the bit pattern found in register X in the memory cell whose address is location.\n" +
                "Example: store reg3 40 would cause the contents of register 5 to be placed in the memory cell whose address is 40."),
            FXCollections.observableArrayList("move", "regX regY",
                "MOVE the bit pattern found in register X to register Y.\n" +
                "Example: move reg9 reg 3 would cause the contents of register 9 to be copied into register 3."),
            FXCollections.observableArrayList("add", "regZ regX regY",
                "ADD the bit patterns in registers X and Y as though they were two’s-complement representations and leave the result in register Z.\n" +
                "Example: add reg3 reg2 reg1 would cause the binary values in registers 1 and 2 to be added and the sum placed in register 3."),
            FXCollections.observableArrayList("addf", "regZ regX regY",
                "ADD the bit patterns in registers X and Y as though they represented floating-point values and leave the result in register Z.\n" +
                "Example: addf reg3 reg4 reg11 would cause the values in registers 4 and 11 to be added as floating-point numbers and the result placed in register 3."),
            FXCollections.observableArrayList("or", "regZ regX regY",
                "OR the bit patterns in registers X and Y and place the result in register Z.\n" +
                "Example: or reg10 reg6 reg2 would cause the result of OR-ing registers 6 and 2 to be placed in register 10."),
            FXCollections.observableArrayList("and", "regZ regX regY",
                "AND the bit patterns in registers X and Y and place the result in register Z.\n" +
                "Example: and reg1 reg4 reg5 would cause the result of AND-ing registers 4 and 5 to be placed in register 1."),
            FXCollections.observableArrayList("xor", "regZ regX regY",
                "XOR the bit patterns in registers X and Y and place the result in register Z.\n" +
                "Example: xor reg5 reg6 reg2 would cause the result of XOR-ing registers 6 and 2 to be placed in register 5."),
            FXCollections.observableArrayList("rotate", "regX num ",
                "ROTATE the bit pattern in register X one bit to the right num times, wrapping the low-order bit to the high-order end each time.\n" +
                "Example: rotate reg4 3 would rotate register 4 right by 3 bits."),
            FXCollections.observableArrayList("jump", "regX label",
                "JUMP to the instruction after the given label if register X equals register 0, otherwise continue sequentially.\n" +
                "Example: jump reg4 loop compares register 4 to register 0; if equal, PC = (address of loop)+1."),
            FXCollections.observableArrayList("halt", "N/A",
                "HALT execution.\n" +
                "Example: halt stops the program."),
            FXCollections.observableArrayList("out", "regX",
                "STORE the value stored in register X into memory address 255.\n" +
                "Example: out reg3 would cause the value in register 3 to be placed in memory address 255.")
        );

        TableView<ObservableList<String>> table = new TableView<>(data);

        TableColumn<ObservableList<String>, String> codeCol = new TableColumn<>("Opcode");
        codeCol.setCellValueFactory(cell -> 
            new ReadOnlyStringWrapper(cell.getValue().get(0))
        );
        codeCol.prefWidthProperty().bind(table.widthProperty().multiply(0.08));

        TableColumn<ObservableList<String>, String> opndCol = new TableColumn<>("Operand");
        opndCol.setCellValueFactory(cell -> 
            new ReadOnlyStringWrapper(cell.getValue().get(1))
        );
        opndCol.prefWidthProperty().bind(table.widthProperty().multiply(0.2));

        TableColumn<ObservableList<String>, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cell -> 
            new ReadOnlyStringWrapper(cell.getValue().get(2))
        );
        descCol.prefWidthProperty().bind(table.widthProperty().multiply(0.7));
        descCol.setCellFactory(col -> {
            TableCell<ObservableList<String>, String> cell = new TableCell<>() {
                private final Text text = new Text();
                {
                    text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                    setGraphic(text);
                }
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    text.setText(empty ? "" : item);
                }
            };
            return cell;
        });

        table.getColumns().setAll(codeCol, opndCol, descCol);

        Button close = new Button("Close");
        close.setOnAction(e -> ((Stage)close.getScene().getWindow()).close());
        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10));

        BorderPane root = new BorderPane(table);
        root.setBottom(footer);
        root.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Instruction Set");
        stage.setScene(new Scene(root, 800, 600));
        stage.show();
    }

    private void handleNewProgramButtonClick() {
        macroScene = false;
        if (loadedFile != null) {
            javafx.scene.control.Alert confirmAlert = 
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm New Program");
            confirmAlert.setHeaderText("New Program");
            confirmAlert.setContentText("Are you sure you want to create a new program? Any unsaved changes will be lost.");
            confirmAlert.showAndWait();
            if (confirmAlert.getResult() != javafx.scene.control.ButtonType.OK) {
                return;
            }
        }
        //Create a new TextArea for the user to input the program.
        CodeArea programCodeArea = new CodeArea();
        programCodeArea.getStylesheets().add(
            getClass()
              .getResource("/dissertation/gui/syntax.css")
              .toExternalForm()
        );
        programCodeArea.setPrefWidth(600);
        programCodeArea.setPrefHeight(400);
        programCodeArea.multiPlainChanges()
            .successionEnds(Duration.ofMillis(200))
            .subscribe(__ -> highlightSyntax(programCodeArea));

        //HBox for additional controls
        HBox controls = new HBox();
        controls.setSpacing(10);
        controls.setAlignment(javafx.geometry.Pos.CENTER);
        Button saveButton = new Button("Save as");
        Button closeButton = new Button("Close");

        controls.getChildren().addAll(saveButton, closeButton);

        VBox layout = new VBox();
        layout.setSpacing(20);
        layout.getChildren().addAll(programCodeArea, controls);

        Scene scene = new Scene(layout, 900, 600);
        Stage fileStage = new Stage();
        fileStage.setTitle("New Program");
        fileStage.setScene(scene);
        fileStage.show();

        closeButton.setOnAction(e -> fileStage.close());
        saveButton.setOnAction(e -> {
            if (programCodeArea.getText().isEmpty()) {
                javafx.scene.control.Alert emptyAlert = 
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                emptyAlert.setTitle("Error!");
                emptyAlert.setHeaderText("Empty Program");
                emptyAlert.setContentText("The program is empty. Please enter a program before saving.");
                emptyAlert.showAndWait();
                return;
            }
            javafx.stage.FileChooser saveFileChooser = new javafx.stage.FileChooser();
            saveFileChooser.setTitle("Save Program File");
            saveFileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
            File saveFile = saveFileChooser.showSaveDialog(fileStage);
            if (saveFile != null) {
                try (java.io.FileWriter writer = new java.io.FileWriter(saveFile)) {
                    writer.write(programCodeArea.getText());
                    loadedFile = saveFile;
                    fileStage.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void handleLoadProgramButtonClick() {
        macroScene = false;
        //stage for loading the program
        if (loadedFile == null) {
            Stage stage = new Stage();
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Open Program File");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
        
            loadedFile = fileChooser.showOpenDialog(stage);
        }

        if (loadedFile != null) {
            try (BufferedReader reader = new BufferedReader(new java.io.FileReader(loadedFile))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                
                CodeArea programCodeArea = new CodeArea(sb.toString());
                programCodeArea.setPrefWidth(600);
                programCodeArea.setPrefHeight(400);
                programCodeArea.getStylesheets().add(
                    getClass()
                      .getResource("/dissertation/gui/syntax.css")
                      .toExternalForm()
                );
                highlightSyntax(programCodeArea);

                programCodeArea.multiPlainChanges()
                    .successionEnds(Duration.ofMillis(200))
                    .subscribe(__ -> highlightSyntax(programCodeArea));
                
                HBox controls = new HBox();
                controls.setSpacing(10);
                controls.setAlignment(javafx.geometry.Pos.CENTER);
                Button saveButton = new Button("Save");
                Button loadButton = new Button("Load");
                Button closeButton = new Button("Close");
                controls.getChildren().addAll(saveButton, loadButton, closeButton);
                
                VBox layout = new VBox();
                layout.setSpacing(20);
                layout.getChildren().addAll(programCodeArea, controls);

                Scene scene = new Scene(layout, 900, 600);
                Stage fileStage = new Stage();
                fileStage.setTitle("Loaded Program");
                fileStage.setScene(scene);
                fileStage.show();
                
                closeButton.setOnAction(e -> {
                    loadedProgram = programCodeArea.getText();
                    fileStage.close();
                });
                loadButton.setOnAction(e -> {
                    javafx.scene.control.Alert confirmAlert = 
                        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirm Save");
                    confirmAlert.setHeaderText("Save Program");
                    confirmAlert.setContentText("Do you want to save before you load a new program?");
                    confirmAlert.showAndWait();
                    if (confirmAlert.getResult() == javafx.scene.control.ButtonType.OK) {
                        saveProgram(programCodeArea.getText());
                    }
                    try {
                        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                        fileChooser.setTitle("Open Program File");
                        fileChooser.getExtensionFilters().add(
                            new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt")
                        );
                        loadedFile = fileChooser.showOpenDialog(fileStage);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        if (outputText != null) {
                        } else {
                            System.out.println("Error loading file, No file selected.");
                        }
                    }
                    if (loadedFile != null) {
                        try (BufferedReader newReader = new BufferedReader(new java.io.FileReader(loadedFile))) {
                            StringBuilder newSb = new StringBuilder();
                            String newLine;
                            while ((newLine = newReader.readLine()) != null) {
                                newSb.append(newLine).append("\n");
                            }
                            programCodeArea.replaceText(newSb.toString());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    else {
                        fileStage.close();
                    }
                });
                saveButton.setOnAction(e -> {
                    saveProgram(programCodeArea.getText());
                    loadedProgram = programCodeArea.getText();
                    fileStage.close();
                });
                
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else {
        }
    }

    private void handleCreateMacroButtonClick() {
        macroScene = true;
        Stage stage = new Stage();
        stage.setTitle("Create Macro");

        Label header = new Label("Define a New Macro");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label nameLabel = new Label("Macro Name:");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. INC");

        Label descLabel = new Label("Macro Description:");
        TextField descField = new TextField();
        descField.setPromptText("e.g. Creating a divide macro");

        Label bodyLabel = new Label("Macro Body:");
        CodeArea programCodeArea = new CodeArea();
        programCodeArea.getStylesheets().add(
            getClass()
              .getResource("/dissertation/gui/syntax.css")
              .toExternalForm()
        );
        programCodeArea.setPrefWidth(600);
        programCodeArea.setPrefHeight(400);
        programCodeArea.multiPlainChanges()
            .successionEnds(Duration.ofMillis(200))
            .subscribe(__ -> highlightSyntax(programCodeArea));

        Label moreInfo = new Label("Note: Macros can only work between 2 inputs and 1 output:\n"
            + "to access these registers, use the following syntax:\n"
            + "1. regx for the first register\n"
            + "2. regy for the second register\n"
            + "3. regz for the third register(this is for the output)\n"
            + "The macro body should be a sequence of instructions\n"
            + "Macros can only use registers 12-15\n"
            + "The macro body cannot contain load or store instructions\n"
            + "You cannot use loadi on regx, regy or regz\n");
            
        Button createBtn = new Button("Create");
        Button cancelBtn = new Button("Cancel");

        HBox buttons = new HBox(10, createBtn, cancelBtn);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(10,
            nameLabel, nameField,
            descLabel, descField,
            bodyLabel, programCodeArea,
            moreInfo,
            buttons
        );
        root.setPadding(new Insets(15));

        root.setAlignment(Pos.CENTER);

        cancelBtn.setOnAction(e -> stage.close());

        createBtn.setOnAction(e -> {
            if (nameField.getText().isEmpty()) {
                javafx.scene.control.Alert emptyAlert = 
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                emptyAlert.setTitle("Error!");
                emptyAlert.setHeaderText("Empty Macro Name");
                emptyAlert.setContentText("The macro name is empty. Please enter a name before creating.");
                emptyAlert.showAndWait();
                return;
            }
            if (descField.getText().isEmpty()) {
                javafx.scene.control.Alert emptyAlert = 
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                emptyAlert.setTitle("Error!");
                emptyAlert.setHeaderText("Empty Macro Description");
                emptyAlert.setContentText("The macro description is empty. Please enter a description before creating.");
                emptyAlert.showAndWait();
                return;
            }
            if (programCodeArea.getText().isEmpty()) {
                javafx.scene.control.Alert emptyAlert = 
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                emptyAlert.setTitle("Error!");
                emptyAlert.setHeaderText("Empty Macro Body");
                emptyAlert.setContentText("The macro body is empty. Please enter a body before creating.");
                emptyAlert.showAndWait();
                return;
            }
            if (programCodeArea.getText().contains("load ") || programCodeArea.getText().contains("store")) {
                javafx.scene.control.Alert emptyAlert = 
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                emptyAlert.setTitle("Error!");
                emptyAlert.setHeaderText("Invalid Macro Body");
                emptyAlert.setContentText("The macro body cannot contain load or store instructions.");
                emptyAlert.showAndWait();
                return;
            }  
            if (programCodeArea.getText().contains("reg0") || programCodeArea.getText().contains("reg1") ||
                programCodeArea.getText().contains("reg2") || programCodeArea.getText().contains("reg3") ||
                programCodeArea.getText().contains("reg4") || programCodeArea.getText().contains("reg5") ||
                programCodeArea.getText().contains("reg6") || programCodeArea.getText().contains("reg7") ||
                programCodeArea.getText().contains("reg8") || programCodeArea.getText().contains("reg9") ||
                programCodeArea.getText().contains("reg10") || programCodeArea.getText().contains("reg11")) {
                javafx.scene.control.Alert emptyAlert = 
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                emptyAlert.setTitle("Error!");
                emptyAlert.setHeaderText("Invalid Macro Body");
                emptyAlert.setContentText("The macro body cannot contain reg0 to reg11.");
                emptyAlert.showAndWait();
                return;
            }
            if (programCodeArea.getText().contains("loadi")) {
                String[] lines = programCodeArea.getText().split("\\n");
                for (String line : lines) {
                    if (line.contains("loadi") && (line.contains("regx") || line.contains("regy") || line.contains("regz"))) {
                        javafx.scene.control.Alert emptyAlert = 
                            new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        emptyAlert.setTitle("Error!");
                        emptyAlert.setHeaderText("Invalid Macro Body");
                        emptyAlert.setContentText("The macro body cannot contain loadi on regx, regy or regz.");
                        emptyAlert.showAndWait();
                        return;
                    }
                }
            }
            if (programCodeArea.getText().contains("halt")) {
                javafx.scene.control.Alert emptyAlert = 
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                emptyAlert.setTitle("Error!");
                emptyAlert.setHeaderText("Invalid Macro Body");
                emptyAlert.setContentText("The macro body cannot contain halt.");
                emptyAlert.showAndWait();
                return;
            }
            //for move, add, addf, and, or, xor. do not allow regx, regy to be overwritten
            if (programCodeArea.getText().contains("add") ||
                programCodeArea.getText().contains("addf") || programCodeArea.getText().contains("and") ||
                programCodeArea.getText().contains("or") || programCodeArea.getText().contains("xor")) {
                String[] lines = programCodeArea.getText().split("\\n");
                for (String line : lines) {
                    String[] tokens = line.split("\\s+");
                    String op = tokens[0].toLowerCase();
                    if (op.equals("add") || op.equals("addf") || op.equals("and") ||
                        op.equals("or") || op.equals("xor")) {
                        if (tokens[2].equals("regx") || tokens[1].equals("regy")) {
                            javafx.scene.control.Alert emptyAlert = 
                                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            emptyAlert.setTitle("Error!");
                            emptyAlert.setHeaderText("Invalid Macro Body");
                            emptyAlert.setContentText("The macro body cannot overwrite regx or regy.");
                            emptyAlert.showAndWait();
                            return;
                        }
                    }
                }
            }
            if (programCodeArea.getText().contains("move")) {
                String[] lines = programCodeArea.getText().split("\\n");
                for (String line : lines) {
                    String[] tokens = line.split("\\s+");
                    String op = tokens[0].toLowerCase();
                    if (op.equals("move")) {
                        if (tokens[1].equals("regx") || tokens[1].equals("regy")) {
                            javafx.scene.control.Alert emptyAlert = 
                                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            emptyAlert.setTitle("Error!");
                            emptyAlert.setHeaderText("Invalid Macro Body");
                            emptyAlert.setContentText("The macro body cannot overwrite regx or regy.");
                            emptyAlert.showAndWait();
                            return;
                        }
                    }
                }
            }
            if (nameField.getText().length() > 8) {
                javafx.scene.control.Alert emptyAlert = 
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                emptyAlert.setTitle("Error!");
                emptyAlert.setHeaderText("Invalid Macro Name");
                emptyAlert.setContentText("The macro name cannot be longer than 8 characters.");
                emptyAlert.showAndWait();
                return;
            }
            // make sure the macro name is not an instruction
            String[] instructions = {"load", "loadi", "store", "move", "add", "addf", "and", "or", "xor", "rotate", "jump", "halt"};
            for (String instruction : instructions) {
                if (nameField.getText().equalsIgnoreCase(instruction)) {
                    javafx.scene.control.Alert emptyAlert = 
                        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    emptyAlert.setTitle("Error!");
                    emptyAlert.setHeaderText("Invalid Macro Name");
                    emptyAlert.setContentText("The macro name cannot be the same as an instruction.");
                    emptyAlert.showAndWait();
                    return;
                }
            }
            File programsDir = new File("src/main/resources/dissertation/programs");
            if (!programsDir.exists()) {
                programsDir.mkdirs();
            }
            File file = new File(programsDir,"macros.txt");
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        int colon = line.indexOf(':');
                        if (colon > 0) {
                            String name = line.substring(0, colon).trim();
                            if (name.equals(nameField.getText())) {
                                javafx.scene.control.Alert emptyAlert = 
                                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                                emptyAlert.setTitle("Error!");
                                emptyAlert.setHeaderText("Macro Name Already Exists");
                                emptyAlert.setContentText("The macro name already exists. Please choose a different name.");
                                emptyAlert.showAndWait();
                                return;
                            }
                        }
                    }
                } catch (IOException g) {
                    new Alert(Alert.AlertType.ERROR,
                        "Failed to load macros:\n" + g.getMessage()
                    ).showAndWait();
                }
            }
            saveMacro(nameField.getText(), descField.getText(), programCodeArea.getText());
            stage.close();
        });

        stage.setScene(new Scene(root, 900, 800));
        stage.show();
    }

    @FXML
    private void handleViewMacrosButtonClick() {
        List<Pair<String,String>> list = new ArrayList<>();
        File programsDir = new File("src/main/resources/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs();
        }
        File file = new File(programsDir,"macros.txt");
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    int colon = line.indexOf(':');
                    int brace = line.indexOf('{', colon);
                    if (colon > 0 && brace > colon) {
                        String name = line.substring(0, colon).trim();
                        String desc = line.substring(colon+1, brace).trim();
                        while ((line = br.readLine()) != null && !line.trim().equals("}")) {}
                        list.add(new Pair<>(name, desc));
                    }
                }
            } catch (IOException e) {
                new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Failed to load macros:\n" + e.getMessage()
                ).showAndWait();
            }
        }

        ObservableList<Pair<String,String>> data = FXCollections.observableArrayList(list);
        TableView<Pair<String,String>> table = new TableView<>(data);

        TableColumn<Pair<String,String>, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getKey())
        );

        nameCol.prefWidthProperty().bind(
            table.widthProperty().multiply(0.2)
        );

        TableColumn<Pair<String,String>, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cell -> 
            new SimpleStringProperty(cell.getValue().getValue())
        );

        descCol.prefWidthProperty().bind(
            table.widthProperty().multiply(0.8)
        );

        table.getColumns().setAll(nameCol, descCol);

        Button close = new Button("Close");
        close.setOnAction(e -> ((Stage)close.getScene().getWindow()).close());
        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10));

        BorderPane root = new BorderPane(table);
        root.setBottom(footer);
        root.setPadding(new Insets(10));

        table.setRowFactory(tv -> {
            TableRow<Pair<String,String>> row = new TableRow<>();
            ContextMenu menu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(evt -> {
                Pair<String,String> macro = row.getItem();
                if (macro == null) return;
                table.getItems().remove(macro);
                try { deleteMacroFromFile(macro.getKey()); }
                catch (IOException ex) {
                    new Alert(Alert.AlertType.ERROR, "Could not delete macro:\n" + ex.getMessage())
                    .showAndWait();
                }
            });
            menu.getItems().add(deleteItem);
            row.contextMenuProperty().bind(
            Bindings.when(row.emptyProperty()).then((ContextMenu)null).otherwise(menu)
            );

            //double-click opens a code view
            row.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2 && !row.isEmpty()) {
                Pair<String,String> macro = row.getItem();
                try {
                String body = loadMacroBody(macro.getKey());
                showMacroCodeScene(macro.getKey(), body);
                } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Failed to load macro code:\n" + e.getMessage())
                    .showAndWait();
                }
            }
            });

            return row;
        });
        Stage stage = new Stage();
        stage.setTitle("Defined Macros");
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    public static boolean saveProgram(String program) {
        if (program.isEmpty()) {
            javafx.scene.control.Alert emptyAlert = 
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            emptyAlert.setTitle("Error!");
            emptyAlert.setHeaderText("Empty Program");
            emptyAlert.setContentText("The program is empty. Please enter a program before saving.");
            emptyAlert.showAndWait();
            return false;
        }
        try (java.io.FileWriter writer = new java.io.FileWriter(loadedFile)) {
            writer.write(program);
            loadedProgram = program;
            PREFS.put(KEY_LAST_FILE_PATH, loadedFile.getAbsolutePath());
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void saveMacro(String name, String description, String body) {
        File programsDir = new File("src/main/resources/dissertation/programs");
        File file = new File(programsDir,"macros.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(name + ": " + description + "{");
            writer.newLine();
            writer.write(body);
            writer.newLine();
            writer.write("}");
            writer.newLine();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR,
                "Failed to save macro:\n" + e.getMessage()
            ).showAndWait();
        }
    }

    private void deleteMacroFromFile(String name) throws IOException {
        File programsDir = new File("src/main/resources/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs();
        }
        File file = new File(programsDir,"macros.txt");
        File temp = new File(programsDir,"macros.tmp");
        if (!temp.exists()) {
            temp.createNewFile();
        }
        try (
        BufferedReader br = new BufferedReader(new FileReader(file));
        BufferedWriter bw = new BufferedWriter(new FileWriter(temp))
        ) {
            String line;
            boolean skipping = false;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (!skipping && t.startsWith(name + ":") && t.contains("{")) {
                    skipping = true;
                    continue;
                }
                if (skipping && t.equals("}")) {
                    skipping = false;
                    continue;
                }
                if (!skipping) {
                    bw.write(line);
                    bw.newLine();
                }
            }
        }
        if (!file.delete() || !temp.renameTo(file)) {
            throw new IOException("Failed to replace macros.txt");
        }
}

    private void clearAndAddText(String text) {
        if (text != null && !text.isEmpty() && outputText.getChildren().isEmpty()) {
            outputText.getChildren().clear();
        }
        for (String line : text.split("\\n", -1)) {
            outputText.getChildren().add(new Text(line + "\n"));
        }
    }

    private String loadMacroBody(String name) throws IOException {
        File dir = new File("src/main/resources/dissertation/programs");
        File file = new File(dir, "macros.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inBlock = false;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (!inBlock) {
                    int colon = t.indexOf(':');
                    int brace = t.indexOf('{', colon);
                    if (colon > 0 && brace > colon) {
                        String n = t.substring(0, colon).trim();
                        if (n.equals(name)) {
                            inBlock = true;
                        }
                    }
                } else {
                    if (t.equals("}")) break;
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        }
    }

    private void showMacroCodeScene(String name, String body) {
        macroScene = true;
        Stage stage = new Stage();
        stage.setTitle("Macro: " + name);

        CodeArea codeArea = new CodeArea();
        codeArea.replaceText(body);
        codeArea.getStylesheets().add(
            getClass().getResource("/dissertation/gui/syntax.css").toExternalForm()
        );
        codeArea.setEditable(false);
        codeArea.setMouseTransparent(true);
        codeArea.setFocusTraversable(false);
        codeArea.setContextMenu(null);
        highlightSyntax(codeArea);

        VirtualizedScrollPane<CodeArea> scroll = new VirtualizedScrollPane<>(codeArea);
        scroll.setPrefSize(580, 320);  // initial preferred size

        Label header = new Label("Macro: " + name);
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Button close = new Button("Close");
        close.setOnAction(e -> stage.close());
        HBox footer = new HBox(close);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(header);
        BorderPane.setAlignment(header, Pos.CENTER);
        root.setCenter(scroll);
        root.setBottom(footer);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 600, 400);
        stage.setScene(scene);
        stage.show();
    }

    private void highlightSyntax(CodeArea codeArea) {
        String text = codeArea.getText();
        Matcher m = SYNTAX_PATTERN.matcher(text);
        StyleSpansBuilder<Collection<String>> spans = new StyleSpansBuilder<>();
        int lastEnd = 0;

        //First pass: opcodes, labels, regs, numbers, comments
        while (m.find()) {
            spans.add(Collections.emptyList(), m.start() - lastEnd);

            String style =
                m.group("OP")  != null ? "opcode"
                : m.group("LBL") != null ? "label"
                : m.group("REG") != null ? "register"
                : m.group("NUM") != null ? "number"
                : m.group("IMP") != null ? "import"
                :                          "comment";
            spans.add(Collections.singleton(style), m.end() - m.start());
            lastEnd = m.end();
        }
        spans.add(Collections.emptyList(), text.length() - lastEnd);
        codeArea.setStyleSpans(0, spans.create());

        //ONLY highlight the jump‐target as jump_target
        Pattern jumpTgt = Pattern.compile(
        "\\bjump\\b\\s+\\breg(?:[0-9]|1[0-5])\\b\\s+\\b([A-Za-z_]\\w*)\\b",
        Pattern.CASE_INSENSITIVE
        );
        Matcher jt = jumpTgt.matcher(text);
        while (jt.find()) {
            int start = jt.start(1);
            int end   = jt.end(1);
            codeArea.setStyle(start, end,
                            Collections.singleton("jump_target"));
        }

        if (macroScene) {
            Pattern p = Pattern.compile("\\b(?:regx|regy|regz)\\b",
                                        Pattern.CASE_INSENSITIVE);
            Matcher pm = p.matcher(text);
            while (pm.find()) {
                codeArea.setStyle(pm.start(), pm.end(),
                                Collections.singleton("placeholder"));
            }
            macroScene = false;
        }

        //paints the import name
        List<String> valid = Compiler.listFileMacros();
        List<String> imports = new ArrayList<>();
        Pattern imp = Pattern.compile("\\bimport\\s+([A-Za-z_]\\w*)\\b",
                                    Pattern.CASE_INSENSITIVE);
        Matcher im = imp.matcher(text);
        while (im.find()) {
            String name = im.group(1);
            if (valid.contains(name)) {
                imports.add(name);
                codeArea.setStyle(im.start(1), im.end(1),
                                Collections.singleton("opcode"));
            }
        }

        //highlights the imported macros
        if (!imports.isEmpty()) {
            String namesPattern = valid.stream()
                                    .map(Pattern::quote)
                                    .collect(Collectors.joining("|"));
            Pattern usage = Pattern.compile("(?m)^\\s*(" + namesPattern + ")\\b",
                                            Pattern.CASE_INSENSITIVE);
            Matcher mu = usage.matcher(text);
            while (mu.find()) {
                codeArea.setStyle(mu.start(1), mu.end(1),
                                Collections.singleton("opcode"));
            }
        }
    }
}