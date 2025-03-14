package disertation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
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

    public static void main(String[] args) {
        BrookshearMachine machine = new BrookshearMachine();
        
        // Initialize memory locations with values
        initializeMemory(machine);

        //the file path
        String filename = "app/src/main/java/dissertation/programs/program.txt";

        // Read the program from the file
        String[] textprogram = readProgramFromFile(filename);

        // check to see if the hash of the program has changed, if it has then recompile, if not then load the program
        int[] program = null;
        /*if (checkHashChanged(textprogram)){
            program = loadProgram();
        }
        else{
            // Load send the program to compiler
            program = Compiler.compileToBrookshear(textprogram);
        }*/

        program = Compiler.compileToBrookshear(textprogram);

        // Load the program into memory
        machine.LoadProgram(program);

        // Print memory contents before execution
        machine.printMemory();

        // Execute the program
        machine.execute();

        // Print memory contents after execution
        machine.printMemory();

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
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Skip lines that start with // or are empty
                if (line.trim().isEmpty() || line.trim().startsWith("//")) {
                    continue;
                }
                programList.add(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading program file: " + filename);
            e.printStackTrace();
        }
        // Convert the list to an array
        String[] program = new String[programList.size()];
        for (int i = 0; i < programList.size(); i++) {
            program[i] = programList.get(i);
        }
        return program;
    }

    //checking the hash of compiled_program.txt to ensure it has not changed
    /*public static boolean checkHashChanged(String[] textprogram) {
        int hash = Compiler.doHash(textprogram);
        try (BufferedReader br = new BufferedReader(new FileReader("app/src/main/java/disertation/programs/compiled_program.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("*")) {
                    int fileHash = Integer.parseInt(line.substring(1));
                    return fileHash == hash;
                }
            }
        } catch (IOException e) {
            System.err.println("Error");
            e.printStackTrace();
        }
                return false;
    }

    //loads the compiled code
    public static int[] loadProgram (){
        String filename = "app/src/main/java/disertation/programs/compiled_program.txt";
        List<Integer> programList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Skip lines that start with // or are empty
                if (line.trim().isEmpty() || line.trim().startsWith("*")) {
                    continue;
                }
                //add a 0x to every line
                programList.add(Integer.parseInt(line, 16));
            }
            int[] program = new int[programList.size()];
                for (int i = 0; i < programList.size(); i++) {
                    program[i] = programList.get(i);
                }
            return program;
        } catch (IOException e) {
            System.err.println("Error reading program file: " + filename);
            e.printStackTrace();
        }
        return null;
    }*/
}