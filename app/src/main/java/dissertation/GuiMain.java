package dissertation;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuiMain extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("app/src/main/java/dissertation/resources/gui/MainView.fxml"));
        primaryStage.setTitle("JavaFX GUI Application");
        primaryStage.setScene(new Scene(root, 400, 300));
        //close after 5 seconds
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}