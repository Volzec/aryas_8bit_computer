package dissertation;
import java.util.ArrayList;
import java.util.List;

public class Wire<T> {
    private T signal;
    private final List<Port<T>> sinks = new ArrayList<>();

    //driver calls set() to propagate a new signal
    public void set(T v) {
        signal = v;
        for (Port<T> p : sinks) {
            p.update(v);
        }
    }

    //components use this to hook their InputPorts onto the net
    public void connect(Port<T> sink) {
        sinks.add(sink);
        //initialize with current value
        if (signal != null) sink.update(signal);
    }
}