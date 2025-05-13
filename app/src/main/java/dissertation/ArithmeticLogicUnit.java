package dissertation;

import dissertation.ALU.*;

/**
 * ArithmeticLogicUnit using gate-level BitwiseGate for logical operations.
 */
public class ArithmeticLogicUnit {
    private static final int WIDTH      = 16;
    private static final int MASK       = (1 << WIDTH) - 1;
    private static final int SIGN_BIT   = 1 << (WIDTH - 1);

    private final Bus<Word> aBus;
    private final Bus<Word> bBus;
    private final Bus<Byte> opBus;
    private final Bus<Word> resultBus;
    private final Bus<Boolean> zeroBus;
    private final Bus<Boolean> carryBus;
    private final Bus<Boolean> overflowBus;
    private final Bus<Boolean> negativeBus;

    public ArithmeticLogicUnit(
            Bus<Word> aBus,
            Bus<Word> bBus,
            Bus<Byte> opBus,
            Bus<Word> resultBus,
            Bus<Boolean> zeroBus,
            Bus<Boolean> carryBus,
            Bus<Boolean> negativeBus,
            Bus<Boolean> overflowBus
    ) {
        this.aBus         = aBus;
        this.bBus         = bBus;
        this.opBus        = opBus;
        this.resultBus    = resultBus;
        this.zeroBus      = zeroBus;
        this.carryBus     = carryBus;
        this.overflowBus  = overflowBus;
        this.negativeBus  = negativeBus;
    }

    public void compute() {
        //Sample 16-bit unsigned inputs
        int ua = aBus.sample().wordToHex() & MASK;
        int ub = bBus.sample().wordToHex() & MASK;
        byte opcode = (byte)(opBus.sample().byteToHex() & 0x7);

        //Convert to bytes for gate-level bitwise ops (low and high)
        Byte aLo = new Byte((byte)(ua & 0xFF));
        Byte aHi = new Byte((byte)((ua >>> 8) & 0xFF));
        Byte bLo = new Byte((byte)(ub & 0xFF));
        Byte bHi = new Byte((byte)((ub >>> 8) & 0xFF));

        int result = 0;
        boolean carryOut = false;
        boolean overflow = false;

        switch (opcode) {
            case 0x0: // ADD via ripple-carry adder
                Word aWord = aBus.sample();
                Word bWord = bBus.sample();
                Object[] res  = WordRippleCarryAdder.add(aWord, bWord);
                Word    sum   = (Word)    res[0];
                boolean carry = (Boolean) res[1];

                short sa = (short)aWord.wordToHex(), sb = (short)bWord.wordToHex();
                short sr = (short)sum.wordToHex();
                boolean of = ((sa ^ sb) >= 0) && ((sr ^ sa) < 0);

                result    = sum.wordToHex();
                carryOut  = carry;
                overflow  = of;
                break;
            case 0x1: // AND via gate
                Byte loAnd = BitwiseGate.apply(aLo, bLo, new AndGate());
                Byte hiAnd = BitwiseGate.apply(aHi, bHi, new AndGate());
                result     = ((hiAnd.byteToHex() & 0xFF) << 8) | (loAnd.byteToHex() & 0xFF);
                carryOut   = false;
                overflow   = false;
                break;

            case 0x2: // OR via gate
                Byte loOr = BitwiseGate.apply(aLo, bLo, new OrGate());
                Byte hiOr = BitwiseGate.apply(aHi, bHi, new OrGate());
                result    = ((hiOr.byteToHex() & 0xFF) << 8) | (loOr.byteToHex() & 0xFF);
                carryOut  = false;
                overflow  = false;
                break;

            case 0x3: // XOR via gate
                Byte loXor = BitwiseGate.apply(aLo, bLo, new XorGate());
                Byte hiXor = BitwiseGate.apply(aHi, bHi, new XorGate());
                result     = ((hiXor.byteToHex() & 0xFF) << 8) | (loXor.byteToHex() & 0xFF);
                carryOut   = false;
                overflow   = false;
                break;


            case 0x4: // PASS-A via NOT(OR(NOT(A),0))
                Byte passLo = BitwiseGate.apply(aLo, new Byte((byte)0), new OrGate());
                Byte passHi = BitwiseGate.apply(aHi, new Byte((byte)0), new OrGate());
                result      = ((passHi.byteToHex() & 0xFF) << 8) | (passLo.byteToHex() & 0xFF);
                carryOut    = false;
                overflow    = false;
                break;

            case 0x5: // ROTR
                System.out.println("ROTR: " + ua + " " + ub);
                int sh   = ub & (WIDTH - 1);
                result   = ((ua >>> sh) | (ua << (WIDTH - sh))) & MASK;
                carryOut = false;
                overflow = false;
                System.out.println("ROTR result: " + result);
                break;

            case 0x6: // FLOAT-ADD
                int wordA = aBus.sample().wordToHex();
                int wordB = bBus.sample().wordToHex();
                int fpRes  = halfAdd(wordA, wordB);
                resultBus.drive(new Word(fpRes));
                carryBus.drive(false);
                overflowBus.drive((fpRes & 0x7C00) == 0x7C00);
                zeroBus.drive(fpRes == 0);
                negativeBus.drive((fpRes & 0x8000) != 0);
                break;

            default:
                result   = ua;
                carryOut = false;
                overflow = false;
                break;
        }

        // Drive flags and result
        resultBus.drive(new Word(result & MASK));
        zeroBus.drive((result & MASK) == 0);
        carryBus.drive(carryOut);
        overflowBus.drive(overflow);
        negativeBus.drive((result & SIGN_BIT) != 0);
    }
    private int shiftRightSticky(int mantissa, int shift) {
        int sticky = 0;
        for (int i = 0; i < shift; i++) {
            sticky |= (mantissa & 0x1);
            mantissa >>>= 1;
        }
        return mantissa | sticky;
    }

    //IEEE-754 half-precision add on two 16-bit words.
    private int halfAdd(int ua, int ub) {
        int signA = ua >>> 15,  expA = (ua >>> 10) & 0x1F,  manA = ua & 0x03FF;
        int signB = ub >>> 15,  expB = (ub >>> 10) & 0x1F,  manB = ub & 0x03FF;
        if (expA == 0x1F || expB == 0x1F) {
            // if either is NaN
            if ((manA != 0) || (manB != 0)) return 0x7E01;  // quiet NaN
            // else propagate infinity (with sign)
            return expA == 0x1F && manA == 0
                ? (ua)
                : (ub);
        }
        if (expA != 0) manA |= 0x0400;  // implicit 1
        if (expB != 0) manB |= 0x0400;
        int d = expA - expB;
        if (d > 0) {
            manB = shiftRightSticky(manB, d);
            expB = expA;
        } else if (d < 0) {
            manA = shiftRightSticky(manA, -d);
            expA = expB;
        }
        int expR = expA;
        int manR;
        if (signA == signB) {
            manR = manA + manB;
        } else {
            if (manA >= manB) {
                manR = manA - manB;
            } else {
                manR = manB - manA;
                signA = signB;  // result sign
            }
        }
        if ((manR & 0x0800) != 0) {
            // overflow of mantissa (bit 11 set)
            manR = shiftRightSticky(manR, 1);
            expR++;
        } else {
            // shift left until leading bit is in position 10
            while ((expR > 0) && ((manR & 0x0400) == 0)) {
                manR <<= 1;
                expR--;
            }
        }
        // guard bit is bit 0 after shiftRightSticky above
        int guard = manR & 0x1;
        // round bit would be next bit if we had more precision, but sticky absorbed it
        if (guard == 1) {
            manR += 1;
            if ((manR & 0x0800) != 0) {
                manR >>>= 1;
                expR++;
            }
        }
        if (expR >= 0x1F) {
            // overflow → infinity
            expR = 0x1F;
            manR = 0;
        } else if (expR <= 0) {
            // underflow → zero (denormals could be handled here)
            expR = 0;
            manR = 0;
        }
        int result = (signA << 15) | ((expR & 0x1F) << 10) | (manR & 0x03FF);
        return result;
    }
}