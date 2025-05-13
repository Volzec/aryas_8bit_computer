package dissertation.ALU;

//One-bit AND gate
public class AndGate extends AbstractGate {
    @Override
    protected int getInputCount() { return 2; }

    @Override
    public boolean getOutput() {
        return inputs[0] && inputs[1];
    }
}