package dissertation;

public class IfId {
    boolean valid;
    Byte    pc;
    Word    instr;
    boolean flush;

    public IfId() {
        this.valid = false;
        this.flush = false;
        this.pc    = new Byte(0);
        this.instr = new Word(0);
    }

    public IfId(IfId other) {
        this.valid = other.valid;
        this.flush = other.flush;
        this.pc    = other.pc;
        this.instr = other.instr;
    }

    public boolean isValid() { return valid; }
    public boolean isFlush() { return flush; }
    public Byte    getPc()   { return pc; }
    public Word     getInstr() { return instr; }
}