package dissertation;

public class Registers {
    private Byte[] registers = new Byte[16]; // 16 general-purpose registers
    //REG0 will always be 0 so that it can be used with the jump instructions

    public Registers() {
        // Initialize each element of the registers array with a new Byte object
        for (int i = 0; i < registers.length; i++) {
            registers[i] = new Byte();
        }
    }

    public Byte get(int index) {
        if (index < 0 || index >= registers.length) {
            throw new IndexOutOfBoundsException("Invalid register index: " + index);
        }
        
        return registers[index];
    }

    public void set(int index, Byte value) {
        if (index < 0 || index >= registers.length) {
            throw new IndexOutOfBoundsException("Invalid register index: " + index);
        }
        registers[index] = value;
    }

    public Byte getData(int register) { //needs to get the byte information
        return registers[register];
    }
}