package dissertation;

public class ExMem {
    Byte pc;
    Word instr;
    boolean valid;
    ControlSignals ctrl;
    Word aluOut, regBval;
    Byte dest;
    public boolean aluOverflow;

    public ExMem() {
        this.valid = false;
        this.pc = new Byte(0);
        this.instr = new Word(0);
        this.aluOut = new Word(0);
        this.regBval = new Word(0);
        this.dest = new Byte(0);
        this.ctrl = new ControlSignals();
    }

    public ExMem(ExMem other) {
        this.valid = other.valid;
        this.pc = other.pc;
        this.instr = other.instr;
        this.aluOut = other.aluOut;
        this.regBval = other.regBval;
        this.dest = other.dest;
        this.ctrl = new ControlSignals(other.ctrl);
    }

    public Byte getPc() { return pc; }
    public boolean isValid() { return valid; }
    public ControlSignals getCtrl() { return ctrl; }
    public Word getAluOut() { return aluOut; }
    public Word getRegBval() { return regBval; }
    public Byte getDest() { return dest;}
    public Word getInstr() { return instr; }
}
