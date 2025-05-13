package dissertation.ALU;

//One-bit NOT gate 
public class NotGate extends AbstractGate {
    @Override
    protected int getInputCount() { return 1; }

    @Override
    public boolean getOutput() {
        return !inputs[0];
    }
}
