package dissertation;

public class ControlSignals {
    public Byte regWrite;   // 1 = write-back to register file
    public Byte memRead;    // 1 = read from data memory
    public Byte memWrite;   // 1 = write to data memory
    public Byte memToReg;   // 1 = route memory data to register file
    public Byte aluSrc;     // 1 = ALU operand B comes from immediate
    public Byte branch;     // 1 = this is a branch instruction
    public Byte aluOp;      // code: 0=ADD,1=SUB,2=AND,3=OR, etc.
    public Byte halt;       // 1 = stop execution

    public ControlSignals() {
        // default all zeros
        regWrite = Byte.hexToByte(0);
        memRead  = Byte.hexToByte(0);
        memWrite = Byte.hexToByte(0);
        memToReg = Byte.hexToByte(0);
        aluSrc   = Byte.hexToByte(0);
        branch   = Byte.hexToByte(0);
        aluOp    = Byte.hexToByte(0);
        halt     = Byte.hexToByte(0);
    }

    /** Copy constructor: snapshot all fields. */
    public ControlSignals(ControlSignals other) {
        this.regWrite = other.regWrite;
        this.memRead  = other.memRead;
        this.memWrite = other.memWrite;
        this.memToReg = other.memToReg;
        this.aluSrc   = other.aluSrc;
        this.branch   = other.branch;
        this.aluOp    = other.aluOp;
        this.halt     = other.halt;
    }

    /** Optional: clone() for convenience. */
    @Override
    public ControlSignals clone() {
        return new ControlSignals(this);
    }
}