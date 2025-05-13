package dissertation.ALU;

//Base class for gates with fixed number of inputs.
public abstract class AbstractGate implements Gate {
    protected boolean[] inputs;

    @Override
    public void setInputs(boolean[] inputs) {
        if (inputs.length != getInputCount()) {
            throw new IllegalArgumentException("Expected " + getInputCount() + " inputs");
        }
        this.inputs = inputs.clone();
    }
    @Override
    public abstract boolean getOutput();
    protected abstract int getInputCount();
}