package dissertation.gui.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;

import dissertation.BrookshearMachine;
import dissertation.Main;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {
    private static final Preferences PREFS = Preferences.userNodeForPackage(MainController.class);
    private static final String KEY_LAST_FILE_PATH = "lastProgramFilePath";

    private static String loadedProgram = "";

    private static File loadedFile = null;

    @FXML
    private VBox mainContainer;

    @FXML
    private Label titleLabel;

    @FXML
    private TextArea outputText;

    @FXML
    private Button actionButton;

    @FXML
    private Button loadProgramButton;

    @FXML
    private Button newProgramButton;

    @FXML
    private Button exitButton;

    public void initialize() {
        // Try to load the last program file using preferences.
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
                    //outputText.setText("Loaded program from: " + lastPath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    outputText.setText("Error loading the last used program.");
                }
            } else {
                //outputText.setText("Last used program not found.");
            }
        } else {
            //outputText.setText("No program loaded.");
        }
        
        titleLabel.setText("Arya's Brookshear Machine");
        actionButton.setOnAction(event -> handleActionButtonClick());
        newProgramButton.setOnAction(event -> handleNewProgramButtonClick());
        loadProgramButton.setOnAction(event -> handleLoadProgramButtonClick());
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

    private void handleActionButtonClick() {
        // Call the machine start routine.
        // Depending on your design you might adjust BrookshearStart to return the machine,
        // or if it stores its memory data in a static location, you could retrieve that.
        BrookshearMachine thisMachine = Main.BrookshearStart(loadedProgram);

        // Now, suppose you add a method in your machine or a helper method that formats memory.
        // For demonstration, let’s assume Main exposes a method to get memory as a formatted string.
        //
        // For example:
        // String memoryValues = Main.getFormattedMemoryValues();
        //
        // If you don't have that method, you'll need to implement it in your machine class.

        String memoryValues = Main.getFormattedMemoryValues(thisMachine); // implement this
        outputText.setText(memoryValues);
    }

    private void handleNewProgramButtonClick() {

        if (loadedFile != null) {
            javafx.scene.control.Alert confirmAlert = 
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Confirm New Program");
            confirmAlert.setHeaderText("New Program");
            confirmAlert.setContentText("Are you sure you want to create a new program? Any unsaved changes will be lost.");
            confirmAlert.showAndWait();
            if (confirmAlert.getResult() == javafx.scene.control.ButtonType.OK) {
                return;
            }
        }
        // Create a new TextArea for the user to input the program.
        TextArea programTextArea = new TextArea();
        programTextArea.setEditable(true);
        programTextArea.setPrefRowCount(25);

        // Create an HBox for additional controls
        HBox controls = new HBox();
        controls.setSpacing(10);
        controls.setAlignment(javafx.geometry.Pos.CENTER);
        Button saveButton = new Button("Save as");
        Button closeButton = new Button("Close");

        controls.getChildren().addAll(saveButton, closeButton);

        // Create a layout (VBox) containing the TextArea and controls.
        VBox layout = new VBox();
        layout.setSpacing(20);
        layout.getChildren().addAll(programTextArea, controls);

        // Build and show a new stage with the loaded program.
        Scene scene = new Scene(layout, 900, 600);
        Stage fileStage = new Stage();
        fileStage.setTitle("New Program");
        fileStage.setScene(scene);
        fileStage.show();

        // Close action for the close button.
        closeButton.setOnAction(e -> fileStage.close());
        saveButton.setOnAction(e -> {
            if (programTextArea.getText().isEmpty()) {
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
                    writer.write(programTextArea.getText());
                    outputText.setText("Program saved successfully.");
                    loadedFile = saveFile;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    outputText.setText("Error saving file.");
                }
            }
        });
    }

    private void handleLoadProgramButtonClick() {
        // Create a new stage for loading the program

        if (loadedFile == null) {
            Stage stage = new Stage();
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Open Program File");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt")
            );
        
            // Show open dialog and wait for a selection
            loadedFile = fileChooser.showOpenDialog(stage);
        }

        if (loadedFile != null) {
            try (BufferedReader reader = new BufferedReader(new java.io.FileReader(loadedFile))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                
                // Create a new TextArea with the loaded file contents.
                TextArea programTextArea = new TextArea(sb.toString());
                programTextArea.setEditable(true);
                programTextArea.setPrefRowCount(25);
                
                // Create an HBox for additional controls
                HBox controls = new HBox();
                controls.setSpacing(10);
                controls.setAlignment(javafx.geometry.Pos.CENTER);
                Button saveButton = new Button("Save");
                Button loadButton = new Button("Load");
                Button closeButton = new Button("Close");
                controls.getChildren().addAll(saveButton, loadButton, closeButton);
                
                // Create a layout (VBox) containing the TextArea and controls.
                VBox layout = new VBox();
                layout.setSpacing(20);
                layout.getChildren().addAll(programTextArea, controls);
                
                // Build and show a new stage with the loaded program.
                Scene scene = new Scene(layout, 900, 600);
                Stage fileStage = new Stage();
                fileStage.setTitle("Loaded Program");
                fileStage.setScene(scene);
                fileStage.show();
                
                // Close action for the close button.
                closeButton.setOnAction(e -> {
                    loadedProgram = programTextArea.getText();
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
                        saveProgram(programTextArea.getText());
                    }
                    javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                    fileChooser.setTitle("Open Program File");
                    fileChooser.getExtensionFilters().add(
                        new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt")
                    );
                    loadedFile = fileChooser.showOpenDialog(fileStage);
                    if (loadedFile != null) {
                        try (BufferedReader newReader = new BufferedReader(new java.io.FileReader(loadedFile))) {
                            StringBuilder newSb = new StringBuilder();
                            String newLine;
                            while ((newLine = newReader.readLine()) != null) {
                                newSb.append(newLine).append("\n");
                            }
                            programTextArea.setText(newSb.toString());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            outputText.setText("Error loading file.");
                        }
                    }
                    else {
                        outputText.setText("File selection cancelled.");
                        fileStage.close();
                    }
                });
                saveButton.setOnAction(e -> {
                    saveProgram(programTextArea.getText());
                    fileStage.close();
                });
                
            } catch (IOException ex) {
                ex.printStackTrace();
                outputText.setText("Error loading file.");
            }
        } else {
            outputText.setText("File selection cancelled.");
        }
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
}