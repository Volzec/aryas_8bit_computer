package dissertation.ALU;

//One-bit OR gate
public class OrGate extends AbstractGate {
    @Override
    protected int getInputCount() { return 2; }

    @Override
    public boolean getOutput() {
        return inputs[0] || inputs[1];
    }
}