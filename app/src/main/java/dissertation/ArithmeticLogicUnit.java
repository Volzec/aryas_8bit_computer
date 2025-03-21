package dissertation;

public class ArithmeticLogicUnit {
    public static Byte and(Byte register1, Byte register2) {
        for (int i = 0; i < 8; i++) {
            if (register1.getBit(i) && register2.getBit(i)) {
                register1.setBit(i, true);
            } else {
                register1.setBit(i, false);
            }
        }
        return register1;
    }

    public static Byte or(Byte register1,Byte register2) {
        for (int i = 0; i < 8; i++) {
            if (register1.getBit(i) || register2.getBit(i)) {
                register1.setBit(i, true);
            } else {
                register1.setBit(i, false);
            }
        }
        return register1;
    }
    
    public Byte not(Byte register1) {
        for (int i = 0; i < 8; i++) {
            register1.setBit(i, !register1.getBit(i));
        }
        return register1;
    }

    public static Byte addTC(Byte registerA, Byte registerB) { //add with twos compliment
        int carry = 0;
        Byte temp = new Byte();
        for (int i = 7; i >= 0; i--) {
            int bit1 = registerA.getBit(i) ? 1 : 0;
            int bit2 = registerB.getBit(i) ? 1 : 0;
            int sum = bit1 + bit2 + carry;
            carry = sum >> 1;
            temp.setBit(i, (sum & 1) == 1);
        }
        return temp;
    }

    public static Byte rotate(Byte register1, Byte register2) {
        for (int i = 0; i < 8; i++) {
            register1.setBit(i, register2.getBit(i));
        }
        return register1;
    }

    public static Byte xor(Byte register1, Byte register2) {
        for (int i = 0; i < 8; i++) {
            if (register1.getBit(i) ^ register2.getBit(i)) {
                register1.setBit(i, true);
            } else {
                register1.setBit(i, false);
            }
        }
        return register1;
    }
}
