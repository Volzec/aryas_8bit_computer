package dissertation;

import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Compiler {
    private static final Map<String, String> instructionMap = new HashMap<>();
    private static final String HASH_FILE = "compiled_program.hash";
    private static final String BINARY_FILE = "compiled_program.bin";
    
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
        Map<String, Integer> labelMap = new HashMap<>(); // Store label addresses
        List<String> processedTokens = new ArrayList<>();
        
        int address = 0;
        // First pass: Collect label positions
        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty() || token.startsWith("//")) {
                continue;
            }
            if (token.endsWith(":")) { 
                String label = token.substring(0, token.length() - 1);
                labelMap.put(label, address);
                continue;
            }
            processedTokens.add(token);
            address++;
        }
        
        // Second pass: Convert tokens
        for (String token : processedTokens) {
            String[] parts = token.split(" ");
            String op = parts[0].toLowerCase();
            
            if (instructionMap.containsKey(op)) {
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i].startsWith("reg")) {
                        int regNum = Integer.parseInt(parts[i].substring(3));
                        parts[i] = Integer.toHexString(regNum).toUpperCase();
                    } else if (parts[i].matches("\\d+")) { 
                        parts[i] = String.format("%02X", Integer.parseInt(parts[i]));
                    } else if (labelMap.containsKey(parts[i])) { // Convert labels to addresses
                        parts[i] = String.format("%02X", labelMap.get(parts[i]));
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
                code.append("0"); // Adding padding before the registers to ensure it doesnt break give an invalid instruction
                code.append(parts[1]);
                code.append(parts[2]);
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

                //System.out.println(code);
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

    public static String computeHash(String[] sourceCode) {
        try {
            //firsly remove all the comments
            for (int i = 0; i < sourceCode.length; i++) {
                sourceCode[i] = sourceCode[i].replaceAll("//.*", "");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String programText = String.join("\n", sourceCode); // Combine all lines
            byte[] hashBytes = digest.digest(programText.getBytes());

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error computing hash", e);
        }
    
    }
    public static void saveHash(String hash) {
        File programsDir = new File("app/src/main/java/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs(); // Create the programs directory if it doesn't exist
        }
        File hashFile = new File(programsDir, HASH_FILE);
        try (PrintWriter writer = new PrintWriter(hashFile)) {
            writer.println(hash);
        } catch (FileNotFoundException e) {
            System.err.println("Error writing hash file: " + e.getMessage());
        }
    }

    public static String loadSavedHash() {
        File programsDir = new File("app/src/main/java/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs(); // Create the programs directory if it doesn't exist
        }
        File hashFile = new File(programsDir, HASH_FILE);
        try {
            return new String(Files.readAllBytes(hashFile.toPath())).trim();
        } catch (IOException e) {
            return ""; // If file doesn't exist, return an empty string
        }
    }

    public static boolean shouldRecompile(String[] sourceCode) {
        String newHash = computeHash(sourceCode);
        String oldHash = loadSavedHash();

        if (newHash.equals(oldHash)) {
            System.out.println("No changes detected. Skipping compilation.");
            return false; // No need to recompile
        } else {
            System.out.println("Changes detected. Recompiling...");
            saveHash(newHash);
            return true; // Recompile needed
        }
    }

    public static int[] loadMachineCode(String fileName) {
        File programsDir = new File("app/src/main/java/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs(); // Create the programs directory if it doesn't exist
        }
        
        File file = new File(programsDir, fileName);
        List<Integer> program = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file)) {
            while (fis.available() > 0) {
                int high = fis.read();
                int low = fis.read();
                program.add((high << 8) | low);
            }
        } catch (IOException e) {
            System.err.println("Error reading binary file: " + e.getMessage());
        }
        return program.stream().mapToInt(i -> i).toArray();
    }


    public static int[] compileToBrookshear(String[] sourceCodeLines) {
        if (!shouldRecompile(sourceCodeLines)) {
            System.out.println("Loading machine code from " + BINARY_FILE);
            return loadMachineCode(BINARY_FILE);
        }
        String[] instructions = parse(sourceCodeLines);
        instructions = optimize(instructions);
        saveToBinaryFile(generateBrookshearCode(instructions), BINARY_FILE);
        return generateBrookshearCode(instructions);
    }
}