package dissertation;

public class MemWb {
    Byte pc;
    Word instr;
    boolean valid;
    ControlSignals ctrl;
    Word memData, aluOut;
    Byte dest;
    public boolean aluOverflow;

    public MemWb() {
        this.valid = false;
        this.pc = new Byte(0);
        this.instr = new Word(0);
        this.aluOut = new Word(0);
        this.memData = new Word(0);
        this.dest = new Byte(0);
        this.ctrl = new ControlSignals();
    }

    public MemWb(MemWb other) {
        this.valid = other.valid;
        this.pc = other.pc;
        this.instr = other.instr;
        this.aluOut = other.aluOut;
        this.memData = other.memData;
        this.dest = other.dest;
        this.ctrl = new ControlSignals(other.ctrl);
    }

    public Byte getPc() { return pc; }
    public boolean isValid() { return valid; }
    public ControlSignals getCtrl() { return ctrl; }
    public Word getMemData() { return memData; }
    public Word getAluOut() { return aluOut; }
    public Byte getDest() { return dest; }
    public Word getInstr() { return instr; }

}
