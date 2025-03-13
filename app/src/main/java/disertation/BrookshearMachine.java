package disertation;

import org.checkerframework.checker.units.qual.s;

public class BrookshearMachine {
    private Memory dataMemory;
    private Memory instructionMemory;
    private Registers registers;
    private int pc;

    public BrookshearMachine() { //decided  on using Harvard architecture instead of Von Neumann architecture
        instructionMemory = new Memory();
        dataMemory = new Memory();
        registers = new Registers();
    }

    public Memory GetDataMemory() {
        return dataMemory;
    }

    public Memory GetInstructionMemory() {
        return instructionMemory;
    }

    public void LoadProgram(int[] program) {
        //to load programs, each instruction is going to have to be split into 2 as the memory is a Harvard architecture
        for (int i = 0; i < program.length; i++) {
            Byte opcode = new Byte();
            Byte operand = new Byte();
            Byte address = new Byte();
            address.HexToByte(i);
            
            opcode.HexToByte((program[i] & 0xFF00) >> 8);
            operand.HexToByte(program[i] & 0x00FF);
            instructionMemory.Set(address, opcode);
            dataMemory.Set(address, operand);
        }
    }

    public void execute() {
        int pc = 0; // Program counter
        while (pc < 256) {
            Byte pcByte = new Byte();
            pcByte.HexToByte(pc);
            Byte opcode = instructionMemory.Get(pcByte);
            Byte operand = dataMemory.Get(pcByte);
            pc = pc + 1;
            System.out.println("PC: " + (pc - 1) + ", Instruction: 0x" + Integer.toHexString(opcode.ByteToHex()).toUpperCase() + Integer.toHexString(operand.ByteToHex()).toUpperCase());
            if (!DecodeAndExecute(opcode, operand)) {
                break; // Stop execution if DecodeAndExecute returns false
            }
            PrintRegisters(); // Print register values after each instruction
        }
    }

private boolean DecodeAndExecute(Byte opcode, Byte operand) {
    //System.out.println(opcode.ByteToHex() + ", " + operand.ByteToHex());
    int instruction = opcode.ByteToHex();
    int opcodeHex = instruction >> 4;
    int reg = instruction & 0x0F;
    int operandHex = operand.ByteToHex();

    int highByteValue = (operandHex & 0xF0) >> 4; // First 4 bits
    int lowByteValue = operandHex & 0x0F; // Last 4 bits
    //System.out.println("High byte: " + highByteValue + ", Low byte: " + lowByteValue);
    Byte reg1Byte;
    Byte reg2Byte;

    System.out.println("Instruction: 0x" + Integer.toHexString(instruction).toUpperCase() + 
                       ", Opcode: 0x" + Integer.toHexString(opcodeHex).toUpperCase() + 
                       ", Reg1: " + reg + 
                       ", Operand: 0x" + Integer.toHexString(operandHex).toUpperCase());

    switch (opcodeHex) {
        case 0x1: // LOAD REGISTER WITH CONTENTS FROM THE ADDRESS IN THE MEMORY, [address]
            registers.Set(reg, dataMemory.Get(operand));
            System.out.println("LOAD: " + registers.GetData(reg).ByteToHex());
            break;
        case 0x2: // LOAD REGISTER WITH VALUE FROM THE ADDRESS, [address]
            registers.Set(reg, operand);
            break;
        case 0x3: // STORE
            dataMemory.Set(operand, registers.GetData(reg));
            break;
        case 0x4: // MOVE
            registers.Set(lowByteValue, registers.Get(highByteValue));
            break;
        case 0x5: // ADD two's complement, split the operand into 2 parts
            reg1Byte = registers.GetData(highByteValue);
            reg2Byte = registers.GetData(lowByteValue);
            Byte result = ArithmeticLogicUnit.addTC(reg1Byte, reg2Byte);
            registers.Set(reg, result);
            break;
        case 0x6: // ADD FLOATING POINT (not implemented)
            break;
        case 0x7: // OR
            reg1Byte = registers.GetData(highByteValue);
            reg2Byte = registers.GetData(lowByteValue);
            result = ArithmeticLogicUnit.or(reg1Byte, reg2Byte);
            registers.Set(reg, result);
            break;
        case 0x8: // AND
            reg1Byte = registers.GetData(highByteValue);
            reg2Byte = registers.GetData(lowByteValue);
            result = ArithmeticLogicUnit.and(reg1Byte, reg2Byte);
            registers.Set(reg, result);
            break;
        case 0x9: // XOR
            reg1Byte = registers.GetData(highByteValue);
            reg2Byte = registers.GetData(lowByteValue);
            result = ArithmeticLogicUnit.xor(reg1Byte, reg2Byte);
            registers.Set(reg, result);
            break;
        case 0xA: // ROTATE
            reg2Byte = registers.GetData(lowByteValue);
            result = ArithmeticLogicUnit.rotate(registers.Get(reg), reg2Byte);
            registers.Set(reg, result);
            break;
        case 0xB: // JUMP
            if (registers.Get(0).ByteToHex() == registers.Get(reg).ByteToHex()) {
                pc = registers.Get(reg).ByteToHex(); // Set the program counter to the value in the register
                return true; // Continue execution
            }
            break;
        case 0xC: // HALT
            return false; // Stop execution
        case 0xF: // OUT
            System.out.println("OUT: " + registers.GetData(reg).ByteToHex());
            break;
        default:
            System.out.println("Unknown opcode: " + opcodeHex);
            return false; // Stop execution on unknown opcode
    }
    return true; // Continue execution
}

    private void PrintRegisters() {
        System.out.print("Registers: ");
        for (int i = 0; i < 16; i++) {
            System.out.print("R" + i + ": " + (registers.Get(i)).ByteToHex() + " ");
        }
        System.out.println();
    }

    public void printMemory() {
        instructionMemory.PrintChangedMemory();
        dataMemory.PrintChangedMemory();
    }
}