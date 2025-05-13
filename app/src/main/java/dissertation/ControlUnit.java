package dissertation;

public class ControlUnit {
    //Decode 4-bit opcode (0x1…0xC) into control signals.
    public ControlSignals decode(Byte opcodeB) {
        int opcode = opcodeB.byteToHex() & 0xF;
        ControlSignals c = new ControlSignals();
    
        switch (opcode) {
            case 0x0: // NOP — do nothing, all control bits stay at zero.
                break;
            case 0x1: // LOAD
                c.regWrite = Byte.hexToByte(1);
                c.memRead  = Byte.hexToByte(1);
                c.memWrite = Byte.hexToByte(0);
                c.memToReg = Byte.hexToByte(1);
                c.aluSrc   = Byte.hexToByte(1); 
                c.branch   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(0);
                break;
    
            case 0x2: // LOADI
                c.regWrite = Byte.hexToByte(1);
                c.memRead  = Byte.hexToByte(0);
                c.memWrite = Byte.hexToByte(0);
                c.memToReg = Byte.hexToByte(0); 
                c.aluSrc   = Byte.hexToByte(1);
                c.branch   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(0);
                break;
    
            case 0x3: // STORE
                c.regWrite = Byte.hexToByte(0);
                c.memRead  = Byte.hexToByte(0);
                c.memWrite = Byte.hexToByte(1);
                c.memToReg = Byte.hexToByte(0);
                c.aluSrc   = Byte.hexToByte(1);
                c.branch   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(0);
                break;
        
            case 0x4: // MOVE S ← R
                c.regWrite = Byte.hexToByte(1);
                c.memRead  = Byte.hexToByte(0);
                c.memWrite = Byte.hexToByte(0);
                c.memToReg = Byte.hexToByte(0); 
                c.aluSrc   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(4);
                c.branch   = Byte.hexToByte(0);
                break;
    
            case 0x5: // ADD
                c.regWrite = Byte.hexToByte(1);
                c.memRead  = Byte.hexToByte(0);
                c.memWrite = Byte.hexToByte(0);
                c.memToReg = Byte.hexToByte(0);
                c.aluSrc   = Byte.hexToByte(0);
                c.branch   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(0);
                break;

            case 0x6: // FPADD
                c.regWrite = Byte.hexToByte(1);
                c.aluSrc   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(6);
                break;
            case 0x7: // OR
                c.regWrite = Byte.hexToByte(1);
                c.aluSrc   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(2);
                break;
            case 0x8: // AND
                c.regWrite = Byte.hexToByte(1);
                c.aluSrc   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(1);
                break;
            case 0x9: // XOR
                c.regWrite = Byte.hexToByte(1);
                c.aluSrc   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(3);
                break;
            case 0xA: // ROTR
                c.regWrite = Byte.hexToByte(1);
                c.aluSrc   = Byte.hexToByte(1);
                c.aluOp    = Byte.hexToByte(5);
                break;
            case 0xB: // BEQ   if R==0 PC←XY
                c.regWrite = Byte.hexToByte(0);
                c.memRead  = Byte.hexToByte(0);
                c.memWrite = Byte.hexToByte(0);
                c.memToReg = Byte.hexToByte(0);
                c.aluSrc   = Byte.hexToByte(0);
                c.aluOp    = Byte.hexToByte(4);
                c.branch   = Byte.hexToByte(1);
                break;
            case 0xC: // HALT
                c.halt     = Byte.hexToByte(1);
                break;
            default:
                //throw new IllegalArgumentException("Illegal opcode: " + c);
                break;
        }
        return c;
    }
}