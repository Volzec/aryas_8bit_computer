package dissertation.ALU;

public class WordRippleCarryAdder {
    /**
     * Adds two 16-bit words, returns result word and carry-out.
     * @param a first operand
     * @param b second operand
     * @return array [resultWord, carryOut]
     */
    public static Object[] add(dissertation.Word a, dissertation.Word b) {
        //Low byte
        dissertation.Byte aLo = new dissertation.Byte((byte)(a.wordToHex() & 0xFF));
        dissertation.Byte bLo = new dissertation.Byte((byte)(b.wordToHex() & 0xFF));
        Object[] loRes = ByteRippleCarryAdder.add(aLo, bLo, false);
        dissertation.Byte sumLo = (dissertation.Byte) loRes[0];
        boolean carryLo = (boolean) loRes[1];
        
        //High byte with carry-in
        dissertation.Byte aHi = new dissertation.Byte((byte)((a.wordToHex() >>> 8) & 0xFF));
        dissertation.Byte bHi = new dissertation.Byte((byte)((b.wordToHex() >>> 8) & 0xFF));
        Object[] hiRes = ByteRippleCarryAdder.add(aHi, bHi, carryLo);
        dissertation.Byte sumHi = (dissertation.Byte) hiRes[0];
        boolean carryOut = (boolean) hiRes[1];

        //Pack back into Word
        int result = ((sumHi.byteToHex() & 0xFF) << 8) | (sumLo.byteToHex() & 0xFF);
        return new Object[]{ new dissertation.Word(result), carryOut };
    }
}

