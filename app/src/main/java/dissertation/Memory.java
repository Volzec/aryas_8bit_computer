package dissertation;

public class Memory {
    private Byte[] memory = new Byte[256]; // 256 memory locations

    public Memory() {
        // Initialize each element of the memory array with a new Byte object
        for (int i = 0; i < memory.length; i++) {
            memory[i] = new Byte();
        }
    }

    public Byte get(Byte address) {
        return memory[address.byteToHex()]; // Return the stored value
    }

    public void set(Byte address, Byte value) {
        memory[address.byteToHex()] = value; // Store the value
        //System.out.println("Memory location 0x" + Integer.toHexString(address.ByteToHex()).toUpperCase() + " set to 0x" + Integer.toHexString(value.ByteToHex()).toUpperCase());
    }

    public void printChangedMemory() {
        System.out.println("Memory contents:");
        for (int i = 0; i < memory.length; i++) {
            if (!memory[i].isEmpty()) {
                System.out.println("0x" + String.format("%02X", i) + ": 0x" + String.format("%02X", memory[i].byteToHex()));
            }
        }
    }

    public int size() {
        return memory.length;
    }
}