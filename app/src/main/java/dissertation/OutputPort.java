package dissertation;

public class OutputPort<T> {
    private final Wire<T> wire;
    public OutputPort(Wire<T> wire) {
        this.wire = wire;
    }
    //the component calls this to drive the net
    public void drive(T value) {
        wire.set(value);
    }
}
