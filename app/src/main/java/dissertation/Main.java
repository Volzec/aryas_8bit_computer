package dissertation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import javafx.application.Application;

public class Main extends Application {
    /* ALL CURRENT OPCODES
    0x1: LOAD DIRECT
    0x2: LOAD INDIRECT
    0x3: STORE
    0x4: MOVE
    0x5: ADD two's complement
    0x6: ADD FLOATING POINT
    0x7: OR
    0x8: AND
    0x9: XOR
    0xA: ROTATE
    0xB: JUMP
    0xC: HALT
    0xF: OUT
    format for instructions: 0x[opcode(0-F)][register1][operand(00-FF)]
    */

    public static void BrookshearStart() {
        System.out.println("Starting the Brookshear Machine");

        BrookshearMachine machine = new BrookshearMachine();
        
        // Initialize memory locations with values
        initializeMemory(machine);

        //the file path
        
        String filename = "/dissertation/programs/program.txt";

        // Read the program from the file
        String[] textprogram = readProgramFromFile(filename);

        // check to see if the hash of the program has changed, if it has then recompile, if not then load the program

        int[] program = Compiler.compileToBrookshear(textprogram);

        // Load the program into memory
        machine.LoadProgram(program);

        // Print memory contents before execution
        //machine.printMemory();

        // Execute the program
        machine.execute();

        // Print memory contents after execution
        //machine.printMemory();

        //machine.GetDataMemory().Get(HexToByte(0x2A)).PrintByte();
    }

    private static void initializeMemory(BrookshearMachine machine) {
        machine.GetDataMemory().Set(HexToByte(0x2A), HexToByte(42)); // Example value
        machine.GetDataMemory().Set(HexToByte(0x2B), HexToByte(10));  // Example value
    }

    private static Byte HexToByte(int hex) {
        Byte b = new Byte();
        b.HexToByte(hex);
        return b;
    }

    private static String[] readProgramFromFile(String filename) {
        List<String> programList = new ArrayList<>();
        // Use getResourceAsStream so it loads the file from the classpath
        try (InputStream in = Main.class.getResourceAsStream(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("//")) {
                    continue;
                }
                programList.add(line);
            }
        } catch (IOException | NullPointerException e) {
            System.err.println("Error reading program file: " + filename);
            e.printStackTrace();
        }
        String[] program = new String[programList.size()];
        for (int i = 0; i < programList.size(); i++) {
            program[i] = programList.get(i);
        }
        return program;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        URL resourceUrl = getClass().getResource("/dissertation/gui/MainView.fxml");
        Parent root = FXMLLoader.load(resourceUrl);
        primaryStage.setTitle("JavaFX GUI Application");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}