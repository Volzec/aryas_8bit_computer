package dissertation.ALU;

public class FullAdder {
    /**
     * Computes sum bit and carry-out for one-bit addition.
     * @param a input bit A
     * @param b input bit B
     * @param cin carry-in bit
     * @return two-element array: [sum, carryOut]
     */
    public static boolean[] compute(boolean a, boolean b, boolean cin) {
        boolean sum = (a ^ b) ^ cin;
        boolean cout = (a && b) || (b && cin) || (a && cin);
        return new boolean[]{sum, cout};
    }
}

