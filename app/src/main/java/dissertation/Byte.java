package dissertation;

public class Byte {
    private boolean[] bits = new boolean[8];

    public Byte() {
        // Default to 0 (all bits false)
    }

    public Byte(int hex) {
        for (int i = 0; i < 8; i++) {
            bits[i] = (hex & (1 << (7 - i))) != 0;
        }
    }

    public void setBit(int index, boolean value) {
        bits[index] = value;
    }

    public boolean getBit(int index) {
        return bits[index];
    }

    public int byteToHex() {
        int value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 1) | (bits[i] ? 1 : 0);
        }
        return value;
    }

    public int byteToHexString() {
        return Integer.parseInt(String.format("%02X", byteToHex()), 16);
    }

    public static Byte hexToByte(int hex) {
        return new Byte(hex);
    }

    public void printByte() {
        for (int i = 0; i < 8; i++) {
            System.out.print(bits[i] ? 1 : 0);
        }
        System.out.println();
    }

    public boolean isEmpty() {
        for (int i = 0; i < 8; i++) {
            if (bits[i]) {
                return false;
            }
        }
        return true;
    }

    public int byteToDecimal() {
        int value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 1) | (bits[i] ? 1 : 0);
        }
        return value;
    }
}