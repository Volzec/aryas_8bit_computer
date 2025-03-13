package disertation;

public class Byte {
    private boolean[] bits = new boolean[8];

    public void SetBit(int index, boolean value) {
        bits[index] = value;
    }
    public boolean GetBit(int index) {
        return bits[index];
    }

    public int ByteToHex (){
        int decimal = 0;
        for (int i = 0; i < 8; i++) {
            if (bits[i]) {
                decimal += Math.pow(2, 7 - i);
            }
        }
        return decimal;
    }

    public Byte HexToByte (int hex) {
        for (int i = 0; i < 8; i++) {
            bits[i] = (hex & (1 << (7 - i))) != 0;
        }
        return this;
    }

    public void PrintByte() {
        for (int i = 0; i < 8; i++) {
            System.out.print(bits[i] ? 1 : 0);
        }
        System.out.println();
    }

    public Boolean IsEmpty() {
        for (int i = 0; i < 8; i++) {
            if (bits[i]) {
                return false;
            }
        }
        return true;
    }
}
