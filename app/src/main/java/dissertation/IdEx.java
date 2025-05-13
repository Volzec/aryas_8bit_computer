package dissertation;

public class IdEx {
    boolean valid;
    ControlSignals ctrl;
    Byte pc;
    Word instr;
    Word imm;
    Byte regA, regB, dest;
    boolean flush;

    public IdEx() {
        this.valid = false;
        this.flush = false;
        this.pc = new Byte(0);
        this.instr = new Word(0);
        this.imm = new Word(0);
        this.regA = new Byte(0);
        this.regB = new Byte(0);
        this.dest = new Byte(0);
        this.ctrl = new ControlSignals();
    }

    public IdEx(IdEx other) {
        this.valid = other.valid;
        this.flush = other.flush;
        this.pc = other.pc;
        this.instr = other.instr;
        this.imm = other.imm;
        this.regA = other.regA;
        this.regB = other.regB;
        this.dest = other.dest;
        this.ctrl = new ControlSignals(other.ctrl);
    }

    public boolean isValid() { return valid; }
    public boolean isFlush() { return flush; } 
    public Byte getPc()   { return pc; }
    public Word getImm() { return imm; }
    public Byte getRegA() { return regA; }
    public Byte getRegB() { return regB; }
    public Byte getDest() { return dest; }
    public ControlSignals getCtrl() { return ctrl; }
    public Word getInstr() { return instr; }
}