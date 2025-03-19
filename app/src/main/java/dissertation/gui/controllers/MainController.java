package dissertation.gui.controllers;

import dissertation.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MainController {

    @FXML
    private VBox mainContainer;

    @FXML
    private Label titleLabel;

    @FXML
    private Button actionButton;

    public void initialize() {
        titleLabel.setText("Welcome to the Java GUI Application");
        actionButton.setOnAction(event -> handleActionButtonClick());
    }

    private void handleActionButtonClick() {
        Main.BrookshearStart();
        titleLabel.setText("Running the Brookshear Machine!");

    }
}