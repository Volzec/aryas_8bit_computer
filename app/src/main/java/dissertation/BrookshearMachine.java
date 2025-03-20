package dissertation;

public class BrookshearMachine {
    private Memory dataMemory;
    private Memory instructionMemory;
    private Registers registers;
    private int pc;

    public BrookshearMachine() { //decided  on using Harvard architecture instead of Von Neumann architecture
        instructionMemory = new Memory();
        dataMemory = new Memory();
        registers = new Registers();
        pc = 0;
    }

    public Memory getDataMemory() {
        return dataMemory;
    }

    public Memory getInstructionMemory() {
        return instructionMemory;
    }

    public 

    public void loadProgram(int[] program) {
        //to load programs, each instruction is going to have to be split into 2 as the memory is a Harvard architecture
        for (int i = 0; i < program.length; i++) {
            Byte opcode = new Byte();
            Byte operand = new Byte();
            Byte address = new Byte();
            address.hexToByte(i);
            
            opcode.hexToByte((program[i] & 0xFF00) >> 8);
            operand.hexToByte(program[i] & 0x00FF);
            instructionMemory.set(address, opcode);
            dataMemory.set(address, operand);
        }
    }

    public void execute() {
        while (pc < 256) {
            Byte pcByte = new Byte();
            pcByte.hexToByte(pc);
            Byte opcode = instructionMemory.get(pcByte);
            Byte operand = dataMemory.get(pcByte);
            pc = pc + 1;
            System.out.println("PC: " + (pc - 1) + ", Instruction: 0x" + String.format("%02X", opcode.byteToHex()).toUpperCase() + String.format("%02X", operand.byteToHex()).toUpperCase());
            if (!decodeAndExecute(opcode, operand)) {
                break; // Stop execution if DecodeAndExecute returns false
            }
            printRegisters(); // Print register values after each instruction
        }
    }

    private boolean decodeAndExecute(Byte opcode, Byte operand) {
        //System.out.println(opcode.byteToHex() + ", " + operand.byteToHex());
        int instruction = opcode.byteToHex();
        int opcodeHex = instruction >> 4;
        int reg = instruction & 0x0F;
        int operandHex = operand.byteToHex();

        int highByteValue = (operandHex & 0xF0) >> 4; // First 4 bits
        int lowByteValue = operandHex & 0x0F; // Last 4 bits
        //System.out.println("High byte: " + highByteValue + ", Low byte: " + lowByteValue);
        Byte reg1Byte;
        Byte reg2Byte;

        System.out.println("Instruction: 0x" + Integer.toHexString(instruction).toUpperCase() + 
                        ", Opcode: 0x" + Integer.toHexString(opcodeHex).toUpperCase() + 
                        ", Reg1: " + reg + 
                        ", Operand: 0x" + String.format("%02X", operandHex).toUpperCase());

        switch (opcodeHex) {
            case 0x1: // LOAD REGISTER WITH CONTENTS FROM THE ADDRESS IN THE MEMORY, [address]
                registers.set(reg, dataMemory.get(operand));
                System.out.println("LOAD: " + registers.getData(reg).byteToHex());
                break;
            case 0x2: // LOAD REGISTER WITH VALUE FROM THE ADDRESS, [address]
                registers.set(reg, operand);
                break;
            case 0x3: // STORE
                dataMemory.set(operand, registers.getData(reg));
                break;
            case 0x4: // MOVE
                registers.set(lowByteValue, registers.get(highByteValue));
                break;
            case 0x5: // ADD two's complement, split the operand into 2 parts
                reg1Byte = registers.getData(highByteValue);
                reg2Byte = registers.getData(lowByteValue);
                Byte result = ArithmeticLogicUnit.addTC(reg1Byte, reg2Byte);
                registers.set(reg, result);
                break;
            case 0x6: // ADD FLOATING POINT (not implemented)
                break;
            case 0x7: // OR
                reg1Byte = registers.getData(highByteValue);
                reg2Byte = registers.getData(lowByteValue);
                result = ArithmeticLogicUnit.or(reg1Byte, reg2Byte);
                registers.set(reg, result);
                break;
            case 0x8: // AND
                reg1Byte = registers.getData(highByteValue);
                reg2Byte = registers.getData(lowByteValue);
                result = ArithmeticLogicUnit.and(reg1Byte, reg2Byte);
                registers.set(reg, result);
                break;
            case 0x9: // XOR
                reg1Byte = registers.getData(highByteValue);
                reg2Byte = registers.getData(lowByteValue);
                result = ArithmeticLogicUnit.xor(reg1Byte, reg2Byte);
                registers.set(reg, result);
                break;
            case 0xA: // ROTATE
                reg2Byte = registers.getData(lowByteValue);
                result = ArithmeticLogicUnit.rotate(registers.get(reg), reg2Byte);
                registers.set(reg, result);
                break;
            case 0xB: // JUMP
                if (registers.get(0).byteToHex() == registers.get(reg).byteToHex()) {
                    pc = operand.byteToHex(); // set the program counter to the value in the operand
                    return true; // Continue execution
                }
                break;
            case 0xC: // HALT
                return false; // Stop execution
            case 0xF: // OUT
                System.out.println("OUT: " + registers.getData(reg).byteToHex());
                break;
            default:
                System.out.println("Unknown opcode: " + opcodeHex);
                return false; // Stop execution on unknown opcode
        }
        return true; // Continue execution
    }

    private void printRegisters() {
        System.out.print("Registers: ");
        for (int i = 0; i < 16; i++) {
            System.out.print("R" + i + ": " + (registers.get(i)).byteToHex() + " ");
        }
        System.out.println();
    }

    public void printMemory() {
        instructionMemory.printChangedMemory();
        dataMemory.printChangedMemory();
    }
}