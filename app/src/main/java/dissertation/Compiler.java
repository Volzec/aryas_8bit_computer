package dissertation;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Compiler {
    private static final Map<String, String> instructionMap = new HashMap<>();
    private static final String HASH_FILE = "compiled_program.hash";
    private static final String BINARY_FILE = "compiled_program.bin";
    private static final Map<String,String> fileMacros = new LinkedHashMap<>();
    static int mulMacroCounter = 0;
    static boolean userWritten = true; //used so that macros can bypass the register check
    private static boolean compileComplete = true; //used to check if the compilation was successful
    
    static {
        instructionMap.put("load", "1"); //load from memory
        instructionMap.put("loadi", "2"); //load immediate value
        instructionMap.put("store", "3"); //store to memory
        instructionMap.put("move", "4"); //move between registers (more like a copy)
        instructionMap.put("add", "5"); //add two registers
        instructionMap.put("addf", "6"); //add floating point
        instructionMap.put("or", "7"); //bitwise or
        instructionMap.put("and", "8"); //bitwise and
        instructionMap.put("xor", "9"); //bitwise xor
        instructionMap.put("rotate", "A"); //rotate left
        instructionMap.put("jump", "B"); //jump to address in register
        instructionMap.put("halt", "C"); //halt the program
    }

    public static void setcompileComplete(boolean complete) {
        compileComplete = complete;
    }

    public static String[] parse(String[] tokens) {
        //holder for an instruction plus whether it's user-written:
        class TokenRecord {
            final String token;
            final boolean userWritten;
            TokenRecord(String token, boolean userWritten) {
                this.token = token;
                this.userWritten = userWritten;
            }
        }

        //Load disk macros into fileMacros
        loadFileMacros();

        Map<String,Integer>   labelMap   = new HashMap<>();
        Map<String,String>    macroMap   = new LinkedHashMap<>(); // imported & #defined
        List<TokenRecord>     phase1     = new ArrayList<>();

        //expand built-ins, import/#define, inline macros
        for (String raw : tokens) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;

            String[] parts = line.split("\\s+");
            String op = parts[0].toLowerCase();

            //import directive
            if (op.equals("import") && parts.length == 2) {
                String name = parts[1];
                String body = fileMacros.get(name);
                if (body == null) {
                    setcompileComplete(false);
                    throw new IllegalArgumentException("Unknown macro import: " + name);
                }
                macroMap.put(name, body);
                continue;  // no instruction emitted
            }

            //in-program #define
            if (line.startsWith("#define")) {
                String[] def = Arrays.stream(line.split("\\s+"))
                                    .filter(s -> !s.isEmpty())
                                    .toArray(String[]::new);
                if (def.length >= 3) {
                    macroMap.put(def[1], def[2]);
                }
                continue;
            }

            //built-in “out” → “store R FF”
            if (op.equals("out") && parts.length == 2) {
                phase1.add(new TokenRecord("store " + parts[1] + " FF", false));
                continue;
            }

            //inline user-defined macros
            if (macroMap.containsKey(op) && parts.length == 4) {
                // name = e.g. "mul", parts = ["mul","reg3","reg2","reg1"]
                String body = macroMap.get(op);
                String dest = parts[1], in1 = parts[2], in2 = parts[3];

                for (String mline : body.split("\\r?\\n")) {
                    //split into tokens so we only replace whole placeholders
                    String[] mtoks = mline.trim().split("\\s+");
                    for (int j = 0; j < mtoks.length; j++) {
                        switch (mtoks[j]) {
                            case "regz": mtoks[j] = dest; break;
                            case "regx": mtoks[j] = in1;  break;
                            case "regy": mtoks[j] = in2;  break;
                            default:    /* leave it */   break;
                        }
                    }
                    String expandedLine = String.join(" ", mtoks);
                    phase1.add(new TokenRecord(expandedLine, false));
                }
                continue;
            }
            // — a normal, user-written instruction/label —
            phase1.add(new TokenRecord(line, true));
        }

        //collect labels and build a list without labels
        List<TokenRecord> phase2 = new ArrayList<>();
        int address = 0;
        for (TokenRecord rec : phase1) {
            String t = rec.token.trim();
            if (t.isEmpty() || t.startsWith("//")) continue;
            if (t.endsWith(":")) {
                labelMap.put(t.substring(0, t.length()-1), address);
            } else {
                phase2.add(rec);
                address++;
            }
        }

        //encode to final machine tokens with reserved-register check
        List<String> instructions = new ArrayList<>();
        for (TokenRecord rec : phase2) {
            String[] parts = Arrays.stream(rec.token.trim().split("\\s+"))
                                .filter(s -> !s.isEmpty())
                                .toArray(String[]::new);
            String op = parts[0].toLowerCase();

            if (!instructionMap.containsKey(op)) {
                setcompileComplete(false);
                throw new IllegalArgumentException("Invalid opcode: " + op);
            }

            //transform out if still present (just in case)
            if (op.equals("out") && parts.length == 2) {
                parts = ("store " + parts[1] + " FF").split(" ");
                op = parts[0];
            }

            //encode operands
            for (int i = 1; i < parts.length; i++) {
                String p = parts[i];

                //registers
                if (p.startsWith("reg")) {
                    int regNum = Integer.parseInt(p.substring(3));
                    // reserved-reg check *only* when this came from user code
                    if (rec.userWritten
                    && (regNum==0||regNum==12||regNum==13||regNum==14||regNum==15)
                    && i == 1
                    && Set.of("load","loadi","store","move",
                            "add","addf","xor","or","and").contains(op)
                    ) {
                        setcompileComplete(false);
                        throw new IllegalArgumentException(
                        "reg" + regNum + " is reserved and cannot be written to."
                        );
                    }
                    parts[i] = Integer.toHexString(regNum).toUpperCase();

                //immediates
                } else if (p.matches("-?\\d+")) {
                    int lit = Integer.parseInt(p) & 0xFF;
                    parts[i] = String.format("%02X", lit);

                //labels
                } else if (labelMap.containsKey(p)) {
                    parts[i] = String.format("%02X", labelMap.get(p));
                }
            }

            instructions.add(String.join(" ", parts));
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
                compileComplete = false;
                throw new IllegalArgumentException("Invalid opcode: " + parts[0]);
            }
    
            StringBuilder code = new StringBuilder();
            code.append(opcode); // Append opcode
    
            //Handle MOVE instruction separately to correct the register order and placement of "0"
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
                        System.out.println("Invalid instruction: " + instructions[i]);
                        compileComplete = false;
                        throw new IllegalArgumentException("Invalid operand: " + parts[j]);
                    }
                }
                while (code.length() < 4) {
                    code.append("0");
                }
            }
    
            machineCode[i] = Integer.parseInt(code.toString(), 16);
        }
        return machineCode;
    }
    
    
    public static void saveToBinaryFile(int[] machineCode, String fileName) {
        File programsDir = new File("src/main/resources/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs();
        }
        
        File file = new File(programsDir, fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (int instruction : machineCode) {
                fos.write((instruction >> 8) & 0xFF);
                fos.write(instruction & 0xFF);
            }
            System.out.println("Machine code saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            compileComplete = false;
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
            String programText = String.join("\n", sourceCode); //Combine all lines
            byte[] hashBytes = digest.digest(programText.getBytes());

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            compileComplete = false;
            throw new RuntimeException("Error computing hash", e);
        }
    
    }
    public static void saveHash(String hash) {
        File programsDir = new File("src/main/resources/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs();
        }
        File hashFile = new File(programsDir, HASH_FILE);
        try (PrintWriter writer = new PrintWriter(hashFile)) {
            writer.println(hash);
        } catch (FileNotFoundException e) {
            compileComplete = false;
            System.err.println("Error writing hash file: " + e.getMessage());
        }
    }

    public static String loadSavedHash() {
        File programsDir = new File("src/main/resources/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs();
        }
        File hashFile = new File(programsDir, HASH_FILE);
        try {
            return new String(Files.readAllBytes(hashFile.toPath())).trim();
        } catch (IOException e) {
            return ""; //If file doesn't exist, return an empty string
        }
    }

    public static boolean shouldRecompile(String[] sourceCode) {
        String newHash = computeHash(sourceCode);
        String oldHash = loadSavedHash();

        if (newHash.equals(oldHash)) {
            System.out.println("No changes detected. Skipping compilation.");
            return false; //No need to recompile
        } else {
            System.out.println("New hash being constructed");
            return true; //Recompile
        }
    }

    public static int[] loadMachineCode(String fileName) {
        File programsDir = new File("src/main/resources/dissertation/programs");
        if (!programsDir.exists()) {
            programsDir.mkdirs();
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
            compileComplete = false;
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
        if (!compileComplete) {
            System.out.println("Compilation failed. Please check your code.");
            return new int[0];
        }
        saveHash(computeHash(sourceCodeLines));
        int [] machineCode = generateBrookshearCode(instructions);
        saveToBinaryFile(machineCode, BINARY_FILE);
        return machineCode;
    }

    public static String[] deCompileToAssembly(int[] machineCode) {
        Map<String, String> inverseMap = new HashMap<>();
        for (Map.Entry<String, String> entry : instructionMap.entrySet()) {
            inverseMap.put(entry.getValue(), entry.getKey());
        }
        
        String[] assemblyInstructions = new String[machineCode.length];
        for (int i = 0; i < machineCode.length; i++) {
            String hexString = String.format("%04X", machineCode[i]);
            String opcodeHex = hexString.substring(0, 1);
            String mnemonic = inverseMap.get(opcodeHex);
            if (mnemonic == null) {
                mnemonic = "INVALID";
            }
            String operandsHex = hexString.substring(1);
            
            assemblyInstructions[i] = mnemonic + " " + operandsHex;
        }
        return assemblyInstructions;
    }

    public static String deCompileSingleInstruction(int instruction) {
        Map<String, String> inverseMap = new HashMap<>();
        for (Map.Entry<String, String> entry : instructionMap.entrySet()) {
            inverseMap.put(entry.getValue(), entry.getKey());
        }
        
        String hexString = String.format("%04X", instruction);
        String opcodeHex = hexString.substring(0, 1);
        String mnemonic = inverseMap.get(opcodeHex);
        if (mnemonic == null) {
            mnemonic = "INVALID";
        }
        String operandsHex = hexString.substring(1);
        
        String operands;
        if (mnemonic.equals("loadi") && operandsHex.length() == 3) {
            int immDec = Integer.parseInt(operandsHex.substring(1), 16);
            operands = "reg" + operandsHex.charAt(0) + " " + immDec;
        }
        else if (mnemonic.equals("jump") && operandsHex.length() == 3) {
            int addrDec = Integer.parseInt(operandsHex.substring(1), 16);
            operands = "reg" + operandsHex.charAt(0) + " " + addrDec;
        }
        else if (mnemonic.equals("load") && operandsHex.length() == 3) {
            int addrDec = Integer.parseInt(operandsHex.substring(1), 16);
            operands = "reg" + operandsHex.charAt(0) + " " + addrDec;
        }
        else if (mnemonic.equals("store") && operandsHex.length() == 3) {
            int addrDec = Integer.parseInt(operandsHex.substring(1), 16);
            operands = "reg" + operandsHex.charAt(0) + " " + addrDec;
        }
        else if (mnemonic.equals("move") && operandsHex.length() == 3 && operandsHex.charAt(0) == '0') {
            operands = "reg" + operandsHex.charAt(1) + " reg" + operandsHex.charAt(2);
        }
        else if (mnemonic.equals("halt ")) {
            operands = "";
        }
        else {
            operands = decodeOperands(operandsHex);
        }
        
        return mnemonic + " " + operands;
    }

    private static String decodeOperands(String operandsHex) {
        StringBuilder result = new StringBuilder();
        
        if (operandsHex.length() == 1) {
            result.append("reg").append(operandsHex);
        } 
        else if (operandsHex.length() == 2) {
            //Convert a two-digit hex value into decimal
            int decVal = Integer.parseInt(operandsHex, 16);
            result.append(decVal);
        } 
        else if (operandsHex.length() == 3) {
            if (operandsHex.charAt(0) == '0') {
                int decVal = Integer.parseInt(operandsHex.substring(1), 16);
                result.append("reg").append(operandsHex.charAt(1)).append(" ").append(decVal);
            } else {
                for (int i = 0; i < operandsHex.length(); i++) {
                    char c = operandsHex.charAt(i);
                    result.append("reg").append(c).append(" ");
                }
            }
        } 
        else {
            for (int i = 0; i < operandsHex.length(); i += 2) {
                String group = operandsHex.substring(i, Math.min(i + 2, operandsHex.length()));
                if (group.length() == 2) {
                    int decVal = Integer.parseInt(group, 16);
                    result.append(decVal).append(" ");
                } else {
                    result.append("reg").append(group).append(" ");
                }
            }
        }
        
        return result.toString().trim();
    }
    public static void loadFileMacros() {
        fileMacros.clear();
        File macrosDir = new File("src/main/resources/dissertation/programs");
        File f = new File(macrosDir, "macros.txt");
        if (!macrosDir.exists()) {
            macrosDir.mkdirs();
        }
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int colon = line.indexOf(':');
                int brace = line.indexOf('{', colon);
                if (colon > 0 && brace > colon) {
                    String name = line.substring(0, colon).trim();
                    StringBuilder body = new StringBuilder();
                    //read until closing brace
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.equals("}")) break;
                        body.append(line).append('\n');
                    }
                    fileMacros.put(name, body.toString().trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> listFileMacros() {
        List<String> names = new ArrayList<>();
        File macrosDir = new File("src/main/resources/dissertation/programs");
        File file = new File(macrosDir, "macros.txt");
        if (!file.exists()) return names;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int colon = line.indexOf(':');
                int brace = line.indexOf('{', colon);
                if (colon > 0 && brace > colon) {
                    String name = line.substring(0, colon).trim();
                    names.add(name);
                    while ((line = br.readLine()) != null && !line.trim().equals("}")) { }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return names;
    }
}