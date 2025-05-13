package dissertation.ALU;

//One-bit XOR gate
public class XorGate extends AbstractGate {
    @Override
    protected int getInputCount() { return 2; }

    @Override
    public boolean getOutput() {
        return inputs[0] ^ inputs[1];
    }
}