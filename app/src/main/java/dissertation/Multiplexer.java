package dissertation;

public class Multiplexer<T> {
    private final int numInputs;
    private final Bus<Byte> selectBus;    // select is still an 8-bit index
    private final Bus<T>   outputBus;
    private final Bus<T>[] inputBuses;

    @SuppressWarnings("unchecked")
    public Multiplexer(int numInputs, Bus<Byte> selectBus, Bus<T> outputBus) {
        this.numInputs  = numInputs;
        this.selectBus  = selectBus;
        this.outputBus  = outputBus;
        this.inputBuses = (Bus<T>[]) new Bus[numInputs];
        for (int i = 0; i < numInputs; i++) {
            // initialize each input bus to some default value (null or zero)
            this.inputBuses[i] = new Bus<>(null);
        }
    }

    public void update() {
        int idx = selectBus.sample().byteToHex();
        if (idx < 0 || idx >= numInputs) {
            throw new IndexOutOfBoundsException("Invalid select index: " + idx);
        }
        outputBus.drive(inputBuses[idx].sample());
    }

    public Bus<T> getInputBus(int index) {
        if (index < 0 || index >= numInputs) {
            throw new IndexOutOfBoundsException("Invalid input index: " + index);
        }
        return inputBuses[index];
    }

    public Bus<T> getOutputBus() {
        return outputBus;
    }
}