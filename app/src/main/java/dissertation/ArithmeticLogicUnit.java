package disertation;

public class ArithmeticLogicUnit {
    public static Byte and(Byte register1, Byte register2) {
        for (int i = 0; i < 8; i++) {
            if (register1.GetBit(i) && register2.GetBit(i)) {
                register1.SetBit(i, true);
            } else {
                register1.SetBit(i, false);
            }
        }
        return register1;
    }

    public static Byte or(Byte register1,Byte register2) {
        for (int i = 0; i < 8; i++) {
            if (register1.GetBit(i) || register2.GetBit(i)) {
                register1.SetBit(i, true);
            } else {
                register1.SetBit(i, false);
            }
        }
        return register1;
    }
    
    public Byte not(Byte register1) {
        for (int i = 0; i < 8; i++) {
            register1.SetBit(i, !register1.GetBit(i));
        }
        return register1;
    }

    public static Byte addTC(Byte registerA, Byte registerB) { //add with twos compliment
        int carry = 0;
        Byte temp = new Byte();
        for (int i = 7; i >= 0; i--) {
            int bit1 = registerA.GetBit(i) ? 1 : 0;
            int bit2 = registerB.GetBit(i) ? 1 : 0;
            int sum = bit1 + bit2 + carry;
            carry = sum >> 1;
            temp.SetBit(i, (sum & 1) == 1);
        }
        return temp;
    }

    public static Byte rotate(Byte register1, Byte register2) {
        for (int i = 0; i < 8; i++) {
            register1.SetBit(i, register2.GetBit(i));
        }
        return register1;
    }

    public static Byte xor(Byte register1, Byte register2) {
        for (int i = 0; i < 8; i++) {
            if (register1.GetBit(i) ^ register2.GetBit(i)) {
                register1.SetBit(i, true);
            } else {
                register1.SetBit(i, false);
            }
        }
        return register1;
    }
}
