package disertation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

class Compiler {
    // Define a dictionary (hashmap) to map high-level language keywords to assembly instructions
    private static final Map<String, String> assemblyMap = new HashMap<>();
    
    static {
        // Initialize the dictionary with some example mappings
        assemblyMap.put("loadd", "0x1");
        assemblyMap.put("loadi", "0x2");
        assemblyMap.put("store", "0x3");
        assemblyMap.put("move", "0x4");
        assemblyMap.put("add", "0x5");
        assemblyMap.put("addf", "0x6");
        assemblyMap.put("or", "0x7");
        assemblyMap.put("and", "0x8");
        assemblyMap.put("xor", "0x9");
        assemblyMap.put("rotate", "0xA");
        assemblyMap.put("jump", "0xB");
        assemblyMap.put("halt", "0xC");
        assemblyMap.put("out", "0xF");
    }

    // Method to translate a high-level instruction to its corresponding assembly code with two operands
    public String generateAssemblyCode(String instruction, String operand1, String operand2) {
        String assemblyInstruction = assemblyMap.get(instruction.toLowerCase());
        
        // If the instruction exists in the map, generate the corresponding assembly code
        if (assemblyInstruction != null) {
            return assemblyInstruction + " " + convertOperand(operand1) + ", " + convertOperand(operand2);
        } else {
            throw new IllegalArgumentException("Unknown instruction: " + instruction);
        }
    }

    // Overloaded method to translate a high-level instruction to its corresponding assembly code with three operands
    public String generateAssemblyCode(String instruction, String operand1, String operand2, String operand3) {
        String assemblyInstruction = assemblyMap.get(instruction.toLowerCase());
        
        // If the instruction exists in the map, generate the corresponding assembly code
        if (assemblyInstruction != null) {
            return assemblyInstruction + " " + convertOperand(operand1) + ", " + convertOperand(operand2) + ", " + convertOperand(operand3);
        } else {
            throw new IllegalArgumentException("Unknown instruction: " + instruction);
        }
    }

    // Overloaded method to translate a high-level instruction to its corresponding assembly code with one operand
    public String generateAssemblyCode(String instruction, String operand1) {
        String assemblyInstruction = assemblyMap.get(instruction.toLowerCase());
        
        // If the instruction exists in the map, generate the corresponding assembly code
        if (assemblyInstruction != null) {
            return assemblyInstruction + " " + convertOperand(operand1);
        } else {
            throw new IllegalArgumentException("Unknown instruction: " + instruction);
        }
    }

    // Overloaded method to translate a high-level instruction to its corresponding assembly code with no operands
    public String generateAssemblyCode(String instruction) {
        String assemblyInstruction = assemblyMap.get(instruction.toLowerCase());
        
        // If the instruction exists in the map, generate the corresponding assembly code
        if (assemblyInstruction != null) {
            return assemblyInstruction;
        } else {
            throw new IllegalArgumentException("Unknown instruction: " + instruction);
        }
    }

    // Method to convert register names or numeric values to their hexadecimal representation
    private String convertOperand(String operand) {
        if (operand.toLowerCase().startsWith("reg")) {
            return Integer.toHexString(Integer.parseInt(operand.substring(3))).toUpperCase();
        }
        try {
            //make sure that if the value is less that 16 it is padded with a 0
            return String.format("%02X", Integer.parseInt(operand));
        } catch (NumberFormatException e) {
            return operand; // Return the operand as is if it's not a register or numeric value
        }
    }

    // Method to compile a program from high-level instructions to assembly code
    public int[] compileProgram(String[] textprogram) {
        int[] program = new int[textprogram.length];

        for (int i = 0; i < textprogram.length; i++) {
            String line = textprogram[i];
            String[] parts = line.split(" ");
            String instruction = parts[0];
            String operand1 = parts.length > 1 ? parts[1] : null;
            String operand2 = parts.length > 2 ? parts[2] : null;
            String operand3 = parts.length > 3 ? parts[3] : null;

            // Generate the assembly code for the current line
            String assemblyCode;
            if (operand3 != null) {
                assemblyCode = generateAssemblyCode(instruction, operand1, operand2, operand3);
            } else if (operand2 != null) {
                assemblyCode = generateAssemblyCode(instruction, operand1, operand2);
                if (assemblyCode.startsWith("0x4")) { // have to add a 0 to the end of the instruction
                    assemblyCode = assemblyCode.substring(0, 3) + " 0," + assemblyCode.substring(3);
                }
            } else if (operand1 != null) {
                assemblyCode = generateAssemblyCode(instruction, operand1);
                assemblyCode = assemblyCode + " 0, 0";
            } else {
                assemblyCode = generateAssemblyCode(instruction);
                assemblyCode = assemblyCode + " 0, 0, 0";
            }

            System.out.println(assemblyCode);

            // Convert the assembly code to an integer value
            int instructionValue = Integer.parseInt(assemblyCode.replace("0x", "").replace(",", "").replace(" ", ""), 16);
            // Store the instruction value in the program array
            program[i] = instructionValue;
        }
        // Calculate the hash of the code to be stored in the file
        String programString = "";
        for (String line : textprogram) {
            programString += line;
        }
        int programHash = programString.hashCode();

        //Saves the compiled program
        saveCompiledProgramToFile(program, "compiled_program.txt", programHash);

        return program;
    }

    public static int doHash(String[] textprogram) {
        String programString = "";
        for (String line : textprogram) {
            programString += line;
        }
        return programString.hashCode();
    }

    // Method to save the compiled program to a file
    private void saveCompiledProgramToFile(int[] program, String filename, int programHash) {
        File file = new File("app/src/main/java/disertation/programs", filename);
        machineCode(program);
        try {
            if (!file.exists()) {
                boolean fileCreated = file.createNewFile(); // Create the file if it does not exist
                if (fileCreated) {
                    System.out.println("File created: " + file.getAbsolutePath());
                } else {
                    System.err.println("Failed to create file: " + file.getAbsolutePath());
                }
            } else {
                System.out.println("File already exists: " + file.getAbsolutePath());
            }
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("*" + programHash);
                for (int instruction : program) {
                    writer.println(Integer.toHexString(instruction).toUpperCase());
                }
                System.out.println("Program successfully written to file: " + file.getAbsolutePath());
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error writing compiled program to file: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
        }
    }

    public static int[] machineCode(int[] program) {
        int[] machineCode = new int[program.length];
        for (int i = 0; i < program.length; i++) {
            int instruction = program[i];
            //split the instruction into 2 bytes
            int opcode = (instruction & 0xFF00) >> 8;
            int operand = instruction & 0x00FF;
            System.out.println("0x" + Integer.toHexString(opcode).toUpperCase() + Integer.toHexString(operand).toUpperCase());
            
        }
        return machineCode;
    }
}