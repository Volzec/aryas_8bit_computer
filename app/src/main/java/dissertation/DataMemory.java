package dissertation;

import java.util.HashMap;
import java.util.Map;

//the data memory in a harvard architecture cpu
public class DataMemory {
    private final Bus<Byte> addressBus;
    private final Bus<Word> dataBus;
    private final Bus<Boolean> readEnable;
    private final Bus<Boolean> writeEnable;
    private final Word[] memory;

    /**
     * @param addressBus  bus carrying data address
     * @param dataBus     bus for data read/write
     * @param readEnable  control bus: true means perform read
     * @param writeEnable control bus: true means perform write
     */
    public DataMemory(Bus<Byte> addressBus,
                      Bus<Word> dataBus,
                      Bus<Boolean> readEnable,
                      Bus<Boolean> writeEnable) {
        this.addressBus = addressBus;
        this.dataBus     = dataBus;
        this.readEnable  = readEnable;
        this.writeEnable = writeEnable;
        this.memory      = new Word[256];
        // initialize memory bytes to 0x00
        for (int i = 0; i < memory.length; i++) {
            memory[i] = new Word(0);
        }
    }

    public void write(Byte address, Word value) {
        memory[address.byteToHex() & 0xFF] = value;
    }

    public void cycle() {
        int addr = addressBus.sample().byteToHex() & 0xFF;
        // WRITE has priority if both asserted
        if (writeEnable.sample()) {
            // write bus value into memory
            memory[addr] = dataBus.sample();
        }
        if (readEnable.sample()) {
            // drive the dataBus with memory value
            dataBus.drive(memory[addr]);
        }
    }
    
    public void printChangedMemory() {
        System.out.println("DataMemory contents:");
        for (int i = 0; i < memory.length; i++) {
            int val = memory[i].wordToHex();
            if (val != 0) {
                System.out.printf("0x%02X: 0x%02X\n", i, val);
            }
        }
    }

    public void printMemory() {
        System.out.println("DataMemory contents:");
        for (int i = 0; i < memory.length; i++) {
            System.out.printf("0x%02X: 0x%02X\n", i, memory[i].wordToHex());
        }
    }

    public Word sendToOutBus() {
        return memory[memory.length - 1];
    }

    /** total capacity */
    public int size() {
        return memory.length;
    }

    //for displaying in the gui and debugging
    public Map<Integer, Word> readAll() {
        Map<Integer, Word> snapshot = new HashMap<>();
        for (int i = 0; i < memory.length; i++) {
            snapshot.put(i, memory[i]);
        }
        return snapshot;
    }
}