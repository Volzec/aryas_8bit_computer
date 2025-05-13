package dissertation.ALU;

public class ByteRippleCarryAdder {
    /**
     * Adds two bytes with carry-in, returns result byte and carry-out.
     * @param a first operand
     * @param b second operand
     * @param cin initial carry-in
     * @return array [resultByte, carryOut]
     */
    public static Object[] add(dissertation.Byte a, dissertation.Byte b, boolean cin) {
        int aVal = a.byteToHex() & 0xFF;
        int bVal = b.byteToHex() & 0xFF;
        int result = 0;
        boolean carry = cin;
        for (int i = 0; i < 8; i++) {
            boolean ai = ((aVal >> i) & 1) == 1;
            boolean bi = ((bVal >> i) & 1) == 1;
            boolean[] out = FullAdder.compute(ai, bi, carry);
            if (out[0]) result |= (1 << i);
            carry = out[1];
        }
        return new Object[]{ new dissertation.Byte((byte) result), carry };
    }
}