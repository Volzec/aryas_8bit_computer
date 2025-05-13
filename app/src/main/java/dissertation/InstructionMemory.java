package dissertation;

//instruction memory for my harvard CPU
// this is a simple array of bytes, with a read port and a write port
public class InstructionMemory {
    private final Bus<Byte> addressBus;
    private final Bus<Word> dataBus;
    private final Word[] memory;

    /**
     * @param addressBus  The bus carrying the PC/address.
     * @param dataBus     The bus to drive the fetched instruction onto.
     * @param program     An array of Byte objects representing your program.
     */
    public InstructionMemory(Bus<Byte> addressBus,
                             Bus<Word> dataBus,
                             Byte[] program) {
        this.addressBus = addressBus;
        this.dataBus    = dataBus;
        // copy program into local memory, pad with NOPs if necessary
        this.memory     = new Word[256];
        for (int i = 0; i < 256; i++) {
            if (i < program.length) {
                this.memory[i] = new Word(program[i].byteToHex());
            } else {
                this.memory[i] = new Word(0x00);
            }
        }
    }


    public Word read() {
        int addr = addressBus.sample().byteToHex() & 0xFF;
        Word instr = memory[addr];
        dataBus.drive(instr);
        return instr;
    }

    /** Optional helper if you need to poke instructions at runtime */
    public void write(Byte address, Word instruction) {
        memory[address.byteToHex() & 0xFF] = instruction;
    }

    public void printMemory() {
        for (int i = 0; i < memory.length; i++) {
            System.out.printf("0x%02X: %s\n", i, memory[i].wordToHexString());
        }
    }  
}