package dissertation;

public class Bus<T> {
    private T value;
    public Bus(T resetValue) { value = resetValue;}
    public void drive(T v) { value = v; }
    public T sample()       { return value; }
}