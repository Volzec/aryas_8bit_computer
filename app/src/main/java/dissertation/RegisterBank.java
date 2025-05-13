package dissertation;

import java.util.HashMap;
import java.util.Map;

public class RegisterBank {
    private final Word[] registers = new Word[16];
    private final Bus<Word>[] readBuses;
    
    @SuppressWarnings("unchecked")
    public RegisterBank() {
        //Initialize registers and read buses
        readBuses = (Bus<Word>[]) new Bus[16];
        for (int i = 0; i < 16; i++) {
            registers[i] = new Word();
            readBuses[i] = new Bus<>(registers[i]);
        }
        // Ensure register 0 always reads zero
        registers[0] = new Word(0);
        readBuses[0].drive(new Word(0));
        //REGISTER 15, 14, 13, 12 TEMPORARY REGISTERS FOR MACROS, NOT ABLE TO BE WRITTEN TO BY THE USER DIRECTLY
        registers[15] = new Word(0);
        readBuses[15].drive(new Word(0));
        registers[14] = new Word(0);
        readBuses[14].drive(new Word(0));
        registers[13] = new Word(0);
        readBuses[13].drive(new Word(0));
        registers[12] = new Word(0);
        readBuses[12].drive(new Word(0));
    }

    //used for writeback to registers
    public void write(int index, Word value) {
        if (index <= 0 || index >= registers.length) {
            throw new IllegalArgumentException("Invalid or unwritable register index: " + index);
        }
        registers[index] = value;
        readBuses[index].drive(value);
    }

    public Bus<Word> getReadBus(int index) {
        if (index < 0 || index >= readBuses.length) {
            throw new IndexOutOfBoundsException("Invalid register index: " + index);
        }
        return readBuses[index];
    }

    //for debugging purposes
    public Word[] readAllBytes() {
        Word[] snapshot = new Word[16];
        for (int i = 0; i < 16; i++) {
            snapshot[i] = registers[i];
        }
        return snapshot;
    }

    //also for debugging purposes
    public void printRegisters() {
        System.out.print("Register contents: ");
        for (int i = 0; i < registers.length; i++) {
            System.out.printf("R%d: 0x%02X ", i, registers[i].wordToHex());
        }
        System.out.println();
    }

    public Map<Integer, Word> readAll() {
        Map<Integer, Word> snapshot = new HashMap<>();
        for (int i = 0; i < registers.length; i++) {
            snapshot.put(i, registers[i]);
        }
        return snapshot;
    }
}