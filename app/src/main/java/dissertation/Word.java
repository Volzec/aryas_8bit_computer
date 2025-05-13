package dissertation;

public class Word {
    private Byte high; // Most-significant 8 bits
    private Byte low;  // Least-significant 8 bits

    public Word() {
        high = new Byte();
        low = new Byte();
    }

    public Word(int value) {
        // value should be in the range 0x0000 to 0xFFFF
        high = new Byte((value >> 8) & 0xFF);
        low = new Byte(value & 0xFF);
    }

    public int getValue() {
        return (high.byteToHex() << 8) | low.byteToHex();
    }

    @SuppressWarnings("static-access")
    public void setValue(int value) {
        high.hexToByte((value >> 8) & 0xFF);
        low.hexToByte(value & 0xFF);
    }

    public int wordToHex() {
        int hexValue = getValue();
        return hexValue;
    }

    public String wordToHexString() {
        return String.format("0x%04X", getValue());
    }

    public Byte getHigh() {
        return high;
    }

    public Byte getLow() {
        return low;
    }

    public static Word[] intToWords(int[] machineCode) {
        Word[] words = new Word[machineCode.length];
        for (int i = 0; i < machineCode.length; i++) {
            words[i] = new Word(machineCode[i]);
        }
        return words;
    }
}