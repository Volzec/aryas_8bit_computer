package dissertation;

public class Memory {
    private Byte[] memory = new Byte[256]; // 256 memory locations

    public Memory() {
        // Initialize each element of the memory array with a new Byte object
        for (int i = 0; i < memory.length; i++) {
            memory[i] = new Byte();
        }
    }

    public Byte Get(Byte address) {
        return memory[address.ByteToHex()]; // Return the stored value
    }

    public void Set(Byte address, Byte value) {
        memory[address.ByteToHex()] = value; // Store the value
        //System.out.println("Memory location 0x" + Integer.toHexString(address.ByteToHex()).toUpperCase() + " set to 0x" + Integer.toHexString(value.ByteToHex()).toUpperCase());
    }

    public void PrintChangedMemory() {
        System.out.println("Memory contents:");
        for (int i = 0; i < memory.length; i++) {
            if (!memory[i].IsEmpty()) {
                System.out.println("0x" + Integer.toHexString(i).toUpperCase() + ": 0x" + Integer.toHexString(memory[i].ByteToHex()).toUpperCase());
            }
        }
    }
}