package disertation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Compiler {
    private static final Map<String, String> instructionMap = new HashMap<>();
    
    static {
        instructionMap.put("loadd", "1");
        instructionMap.put("loadi", "2");
        instructionMap.put("store", "3");
        instructionMap.put("move", "4");
        instructionMap.put("add", "5");
        instructionMap.put("addf", "6");
        instructionMap.put("or", "7");
        instructionMap.put("and", "8");
        instructionMap.put("xor", "9");
        instructionMap.put("rotate", "A");
        instructionMap.put("jump", "B");
        instructionMap.put("halt", "C");
        instructionMap.put("out", "F");
    }

    public static String[] parse(String[] tokens) {
        List<String> instructions = new ArrayList<>();
        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty() || token.startsWith("//")) {
                continue; // Ignore comments and empty lines
            }
            
            String[] parts = token.split(" ");
            String op = parts[0].toLowerCase();
            
            if (instructionMap.containsKey(op)) {
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].startsWith("reg")) { // Convert register numbers to hex
                        int regNum = Integer.parseInt(parts[i].substring(3));
                        parts[i] = Integer.toHexString(regNum).toUpperCase();
                    } else if (parts[i].matches("\\d+")) { // Convert decimal numbers to two-digit hex
                        parts[i] = String.format("%02X", Integer.parseInt(parts[i]));
                    }
                }
                instructions.add(String.join(" ", parts));
            } else {
                throw new IllegalArgumentException("Syntax error: " + token);
            }
        }
        return instructions.toArray(new String[0]);
    }

    public static String[] optimize(String[] instructions) {
        for (int i = 0; i < instructions.length - 1; i++) {
            if (instructions[i].startsWith("loadd") && instructions[i].equals(instructions[i + 1])) {
                instructions[i] = "REMOVED";
            }
        }
        return java.util.Arrays.stream(instructions)
            .filter(instr -> !instr.equals("REMOVED"))
            .toArray(String[]::new);
    }

    public static int[] generateBrookshearCode(String[] instructions) {
        int[] machineCode = new int[instructions.length];
    
        for (int i = 0; i < instructions.length; i++) {
            String[] parts = instructions[i].split(" ");
            String opcode = instructionMap.get(parts[0]);
    
            if (opcode == null) {
                throw new IllegalArgumentException("Invalid opcode: " + parts[0]);
            }
    
            StringBuilder code = new StringBuilder();
            code.append(opcode); // Append opcode
    
            // Handle MOVE instruction separately to correct the register order and placement of "0"
            if (opcode.equals("4") && parts.length == 3) {
                code.append("0"); // Add padding 0 BEFORE the second register
                code.append(parts[1]); // Destination register
                code.append(parts[2]); // Source register
            } else {
                for (int j = 1; j < parts.length; j++) {
                    if (parts[j].matches("^[0-9A-F]$")) { // Registers (0-F)
                        code.append(parts[j]);
                    } else if (parts[j].matches("^[0-9A-F]{2}$")) { // Memory addresses / Immediate values (00-FF)
                        code.append(parts[j]);
                    } else {
                        throw new IllegalArgumentException("Invalid operand: " + parts[j]);
                    }
                }
                while (code.length() < 4) {
                    code.append("0");
                }

                System.out.println(code);
            }
    
            machineCode[i] = Integer.parseInt(code.toString(), 16);
        }
        return machineCode;
    }
    
    
    public static void saveToBinaryFile(int[] machineCode, String fileName) {
        File programsDir = new File("app/src/main/java/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs(); // Create the programs directory if it doesn't exist
        }
        
        File file = new File(programsDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (int instruction : machineCode) {
                fos.write((instruction >> 8) & 0xFF);
                fos.write(instruction & 0xFF);
            }
            System.out.println("Machine code saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static int[] compileToBrookshear(String[] sourceCodeLines) {
        String[] instructions = parse(sourceCodeLines);
        instructions = optimize(instructions);
        saveToBinaryFile(generateBrookshearCode(instructions), "compiled_program.bin");
        return generateBrookshearCode(instructions);
    }
}




/* 
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
*/