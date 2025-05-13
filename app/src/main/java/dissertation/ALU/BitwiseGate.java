package dissertation.ALU;

import dissertation.Byte;

//Multi-bit gate: apply a one-bit Gate to each bit of two Byte inputs.
public class BitwiseGate implements Gate {
    private final Gate[] bitGates;
    private final Byte inputA;
    private final Byte inputB;
    private boolean[] outputs;

    /**
     * @param bitPrototype prototype gate for each bit (AndGate, OrGate, etc.)
     * @param a first operand
     * @param b second operand (ignored for NOT)
     */
    public BitwiseGate(Gate bitPrototype, Byte a, Byte b) {
        int size = 8;
        this.bitGates = new Gate[size];
        this.inputA = a;
        this.inputB = b;
        for (int i = 0; i < size; i++) {
            try {
                // clone prototype by reflection
                this.bitGates[i] = bitPrototype.getClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to copy gate prototype", e);
            }
        }
        this.outputs = new boolean[size];
    }

    @Override
    public void setInputs(boolean[] inputs) {
        //unused: we sample directly from Byte inputs
    }

    @Override
    public boolean getOutput() {
        throw new UnsupportedOperationException(
            "Use getResult() to retrieve full Byte result"
        );
    }

    public Byte getResult() {
        int aVal = inputA.byteToHex() & 0xFF;
        int bVal = inputB.byteToHex() & 0xFF;
        int res = 0;
        for (int i = 0; i < 8; i++) {
            boolean ai = ((aVal >> i) & 1) == 1;
            boolean bi = ((bVal >> i) & 1) == 1;
            Gate gate = bitGates[i];
            if (gate instanceof NotGate) {
                gate.setInputs(new boolean[]{ ai });
            } else {
                gate.setInputs(new boolean[]{ ai, bi });
            }
            boolean out = gate.getOutput();
            if (out) res |= (1 << i);
        }
        return new Byte((byte)res);
    }

    public static Byte apply(Byte a, Byte b, Gate oneBitGate) {
        int aval = a.byteToHex() & 0xFF;
        int bval = b.byteToHex() & 0xFF;
        int out  = 0;
        for (int i = 0; i < 8; i++) {
            boolean ba = ((aval >>> i) & 1) != 0;
            boolean bb = ((bval >>> i) & 1) != 0;
            oneBitGate.setInputs(new boolean[]{ ba, bb });
            if (oneBitGate.getOutput()) {
                out |= (1 << i);
            }
        }
        return Byte.hexToByte(out);
    }
}