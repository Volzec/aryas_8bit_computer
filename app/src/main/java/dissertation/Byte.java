package dissertation;

public class Byte {
    private boolean[] bits = new boolean[8];

    public void setBit(int index, boolean value) {
        bits[index] = value;
    }
    public boolean getBit(int index) {
        return bits[index];
    }

    public int byteToHex (){
        int decimal = 0;
        for (int i = 0; i < 8; i++) {
            if (bits[i]) {
                decimal += Math.pow(2, 7 - i);
            }
        }
        return decimal;
    }

    public Byte hexToByte (int hex) {
        for (int i = 0; i < 8; i++) {
            bits[i] = (hex & (1 << (7 - i))) != 0;
        }
        return this;
    }

    public void printByte() {
        for (int i = 0; i < 8; i++) {
            System.out.print(bits[i] ? 1 : 0);
        }
        System.out.println();
    }

    public Boolean isEmpty() {
        for (int i = 0; i < 8; i++) {
            if (bits[i]) {
                return false;
            }
        }
        return true;
    }
}
