package dissertation;

import java.util.LinkedHashMap;
import java.util.Map;

//the cpu class, contains all the components of the cpu and the pipeline registers
public class PipelinedCPU {

    public enum PCSel {
        PC_PLUS_4,
        OVERFLOW,
        BRANCH_ZERO,
        BRANCH_NEG
    }


    //Buses
    private final Bus<Byte> pcBus = new Bus<>(Byte.hexToByte(0));
    private final Bus<Byte> branchBus    = new Bus<>(Byte.hexToByte(0));
    private final Bus<PCSel> pcSelBus = new Bus<>(PCSel.PC_PLUS_4);

    private final Bus<Byte> instrAddrBus = new Bus<>(Byte.hexToByte(0));
    private final Bus<Word> instrBus     = new Bus<>(new Word(0));

    private final Bus<Byte> src1SelBus   = new Bus<>(Byte.hexToByte(0));
    private final Bus<Byte> src2SelBus   = new Bus<>(Byte.hexToByte(0));
    private final Bus<Word> immBus       = new Bus<>(new Word(0));
    private final Bus<Byte> aluOpBus     = new Bus<>(Byte.hexToByte(0));
    private final Bus<Boolean> aluSrcBus = new Bus<>(false);

    private final Bus<Boolean> memReadBus = new Bus<>(false);
    private final Bus<Boolean> memWriteBus= new Bus<>(false);
    private final Bus<Byte> memToRegSel    = new Bus<>(Byte.hexToByte(0));
    private final Bus<Boolean> regWriteBus = new Bus<>(false);
    private final Bus<Boolean> branchEnBus = new Bus<>(false);

    private final Bus<Byte> writeBackBus  = new Bus<>(Byte.hexToByte(0));

    private final Bus<Word> aluAinBus     = new Bus<>(new Word(0));
    private final Bus<Word> aluBinBus     = new Bus<>(new Word(0));
    private final Bus<Word> aluOutBus     = new Bus<>(new Word(0));
    private final Bus<Boolean> aluZeroBus = new Bus<>(false);
    private final Bus<Boolean> aluOverflowBus = new Bus<>(false);
    private final Bus<Boolean> aluCarryBus = new Bus<>(false);
    private final Bus<Boolean> aluNegativeBus = new Bus<>(false);
    private final Bus<Word> outBus    = new Bus<>(new Word(0));

    private final Bus<Byte> dataAddrBus   = new Bus<>(Byte.hexToByte(0));
    private final Bus<Word> dataBus       = new Bus<>(new Word(0));

    //Components
    private final InstructionMemory instrMem;
    private final DataMemory        dataMem;
    private final RegisterBank      regFile;
    private final ArithmeticLogicUnit alu;
    @SuppressWarnings("rawtypes")
    private final Multiplexer       src1Mux;
    @SuppressWarnings("rawtypes")
    private final Multiplexer       src2Mux;
    @SuppressWarnings("rawtypes")
    private final Multiplexer       wbMux;
    private final ControlUnit       control;
    private Byte pc = new Byte(-1);

    //Pipeline registers
    private final IfId  if_id  = new IfId();
    private final IdEx  id_ex  = new IdEx();
    private final ExMem ex_mem = new ExMem();
    private final MemWb mem_wb = new MemWb();

    private final ExMem lastExMem = new ExMem();

    //Halt flag
    private final Bus<Boolean> haltBus = new Bus<>(false);
    private boolean halted = false;

    private boolean outputTriggered = false;

    @SuppressWarnings("rawtypes")
    public PipelinedCPU() {
        instrMem = new InstructionMemory(instrAddrBus, instrBus, new Byte[0]);
        dataMem  = new DataMemory(dataAddrBus, dataBus, memReadBus, memWriteBus);
        regFile  = new RegisterBank();
        alu      = new ArithmeticLogicUnit(aluAinBus, aluBinBus, aluOpBus, aluOutBus, aluZeroBus, aluCarryBus, aluNegativeBus, aluOverflowBus);

        src1Mux  = new Multiplexer(16, src1SelBus, aluAinBus);
        src2Mux  = new Multiplexer(16, src2SelBus, aluBinBus);
        wbMux    = new Multiplexer(2,  memToRegSel, writeBackBus);

        control  = new ControlUnit();

        //Initialize pipeline registers
        if_id.valid = id_ex.valid = ex_mem.valid = mem_wb.valid = false;
        id_ex.dest    = Byte.hexToByte(0);
        id_ex.regA    = Byte.hexToByte(0);
        id_ex.regB    = Byte.hexToByte(0);
        id_ex.imm     = new Word(0);
        id_ex.ctrl    = new ControlSignals();
        ex_mem.ctrl  = new ControlSignals();
        mem_wb.ctrl  = new ControlSignals();
        ex_mem.aluOut = new Word(0);
        ex_mem.regBval = new Word(0);
        ex_mem.dest = Byte.hexToByte(0);
        mem_wb.aluOut = new Word(0);
        mem_wb.memData = new Word(0);
        mem_wb.dest = Byte.hexToByte(0);
        if_id.instr = new Word(0);
        if_id.pc    = Byte.hexToByte(0);
        id_ex.pc      = Byte.hexToByte(0);
        mem_wb.pc = Byte.hexToByte(0);
        ex_mem.pc = Byte.hexToByte(0);
    }
    

    public boolean isHalted() {
        return halted; 
    }

    public void autoTick() {
        writeBack();
        if (halted) {
            return;
        }
        memory();
        execute();
        decode();
        fetch();
    }

    public void writeBackStage() {
        writeBack();
    }
    
    public void memoryStage() {
        memory();
    }
    
    public void executeStage() {
        execute();
    }
    
    public void decodeStage() {
        decode();
    }
    
    public void fetchStage() {
        fetch();
    }

    public void fetch() {
        //System.out.println("fetching");
        if (haltBus.sample()) {
            if_id.valid = false;
             return;
        }
        //handle the one-cycle bubble
        boolean flush = if_id.flush;
        if_id.valid = !flush;
        if_id.flush = false;

    
        //decide nextPC from branch or PC+1
        int oldPC    = pc.byteToHex();
        PCSel sel    = pcSelBus.sample();
        Byte  nextPC;
        switch (sel) {
        case PC_PLUS_4:
            nextPC = Byte.hexToByte((oldPC + 1) & 0xFF);
            break;
        case BRANCH_ZERO:  // or BRANCH_NEG if you implement that too
        case BRANCH_NEG:
        case OVERFLOW:
            nextPC = branchBus.sample();
            break;
        default:
            nextPC = Byte.hexToByte((oldPC + 1) & 0xFF);
        }
    
        // If we’ve reached address 0xFF, stop the CPU here
        if (nextPC.byteToHex() == 0xFF) {
            // drive the halt signal so that future fetches/execs are disabled
            haltBus.drive(true);
            halted = true;
            if_id.valid = false;
            return;
        }
        // otherwise, proceed as normal
        pcBus.drive(nextPC);
        pc = nextPC;
        instrAddrBus.drive(nextPC);
        Word inst = instrMem.read();
    
        //latch into IF/ID (unless it’s a flush bubble)
        if (!flush) {
            if_id.pc    = nextPC;
            if_id.instr = inst;
        }
    
        instrBus.drive(inst);
    }
    
    private void decode() {
        src1SelBus.drive(Byte.hexToByte(0));
        src2SelBus.drive(Byte.hexToByte(0));
        immBus    .drive(new Word());

        //clear EX‐stage
        if (id_ex.flush) {
            id_ex.valid = false;
            id_ex.flush = false;
            return;
        }
    
        //handle empty pipeline bubble
        if (!if_id.valid) {
            id_ex.valid = false;
            return;
        }

        //hazard detection
        int src1      = (if_id.instr.wordToHex() >>> 8) & 0xF;
        int src2      = (if_id.instr.wordToHex() >>> 4) & 0xF;
        int loadedReg = id_ex.dest.byteToHex();
        if (id_ex.valid && (id_ex.ctrl.memRead.byteToHex() == 1)
            && (loadedReg == src1 || loadedReg == src2)) {
            id_ex.valid = false;
            return;
        }

        Word a = if_id.instr;

        //upack the 16-bit instruction
        int bits   = a.wordToHex() & 0xFFFF;
        int opcode = (bits >>> 12) & 0xF;
        int nib1   = (bits >>> 8) & 0xF;
        int nib2   = (bits >>> 4) & 0xF;
        int nib3   = bits & 0xF;
        int imm8   = bits & 0xFF;

        Byte opcodeB = Byte.hexToByte(opcode);
        ControlSignals ctrl = control.decode(opcodeB);

        //drive all the control‐signal buses
        haltBus.drive(ctrl.halt.byteToHex() == 1);

    
        int raw8;
        //decode operands & set up the ID/EX latch
        Byte R, S, T;
        switch (opcode) {
        case 0x1: // LOAD  R ← M[XY]
            System.out.println(imm8);
            R = Byte.hexToByte(nib1);
            raw8 = imm8;
            Word addrWord = new Word(raw8);
        
            src1SelBus.drive(Byte.hexToByte(0));
            immBus.drive(addrWord);
            id_ex.dest = R;
            id_ex.regA = Byte.hexToByte(0);
            id_ex.regB = Byte.hexToByte(0);
            id_ex.imm  = addrWord;
            break;
    
        case 0x2: // LOADI R ← sign-extended imm8
            R = Byte.hexToByte(nib1);
            src1SelBus.drive(Byte.hexToByte(0));
        
            //sign-extend bit-7 of imm8 into full 16 bits:
            raw8   = imm8 & 0xFF;
            int sext32 = ( (raw8 << 24) >> 24 );
            Word w     = new Word(sext32 & 0xFFFF);
        
            immBus.drive(w);
            id_ex.dest = R;
            id_ex.regA = Byte.hexToByte(0);
            id_ex.regB = Byte.hexToByte(0);
            id_ex.imm  = w;
            break;
    
        case 0x3: // STORE M[XY] ← R
            R = Byte.hexToByte(nib1);
            int addr3 = ((nib2<<4)|nib3) & 0xFF;
            src1SelBus.drive(Byte.hexToByte(0));  // base = R0
            src2SelBus.drive(R);                  // data = R
            immBus   .drive(new Word(addr3));
            id_ex.dest = Byte.hexToByte(0);
            id_ex.regA = Byte.hexToByte(0);
            id_ex.regB = R;
            id_ex.imm  = new Word(addr3);
            break;
    
        case 0x4: // MOVE S ← R  (instruction bits: 4 0 R S)
            R = Byte.hexToByte(nib2);
            S = Byte.hexToByte(nib3);
        
            src1SelBus.drive(R);
            src2SelBus.drive(Byte.hexToByte(0));
            immBus   .drive(new Word(0));
        
            id_ex.dest = S;
            id_ex.regA = R;
            id_ex.regB = Byte.hexToByte(0);
            id_ex.imm  = new Word(0);
            break;
    
        case 0x5: case 0x6: case 0x7: case 0x8: case 0x9:
            // ADD, FPADD, OR, AND, XOR
            R = Byte.hexToByte(nib1);
            S = Byte.hexToByte(nib2);
            T = Byte.hexToByte(nib3);
            src1SelBus.drive(S);
            src2SelBus.drive(T);
            id_ex.dest = R;
            id_ex.regA = S;
            id_ex.regB = T;
            id_ex.imm  = new Word(0);
            break;
    
        case 0xA: // ROTR
            R = Byte.hexToByte(nib1);
            Word X = new Word(nib3);
            src1SelBus.drive(R);
            immBus   .drive(X);
            id_ex.dest = R;
            id_ex.regA = R;
            id_ex.regB = Byte.hexToByte(0);
            id_ex.imm  = X;
            break;
    
        case 0xB: // BEQ   if R==0 PC←XY
            R = Byte.hexToByte(nib1);
            int  addrB = ((nib2 << 4) | nib3) & 0xFF;
            src1SelBus.drive(R);
            src2SelBus.drive(Byte.hexToByte(0));
            immBus.drive(new Word(addrB));
        
            id_ex.dest = Byte.hexToByte(0);
            id_ex.regA = R;
            id_ex.regB = Byte.hexToByte(0);
            id_ex.imm  = new Word(addrB);
            break;
        case 0xC: // HALT
            R = Byte.hexToByte(nib1);
            src1SelBus.drive(R);
            src2SelBus.drive(Byte.hexToByte(0));
            immBus.drive(new Word(0));
        
            id_ex.dest = Byte.hexToByte(0);
            id_ex.regA = R;
            id_ex.regB = Byte.hexToByte(0);
            id_ex.imm  = new Word(0);
            break;
        default:
            // NOP / illegal
            id_ex.valid = false;
            return;
        }
    
        //latch control and PC into ID/EX
        id_ex.ctrl  = ctrl;
        id_ex.pc    = if_id.pc;
        id_ex.valid = true;
        id_ex.instr = if_id.instr;
    }

    private void execute() {
        aluSrcBus  .drive(id_ex.ctrl.aluSrc  .byteToHex()==1);
        aluOpBus   .drive(id_ex.ctrl.aluOp);
        branchEnBus.drive(id_ex.ctrl.branch .byteToHex()==1);

        boolean isBranch = (id_ex.ctrl.branch.byteToHex() == 1);// control says “this is a branch”
        int imm8    = id_ex.imm.wordToHex() & 0xFF;

        pcSelBus.drive(PCSel.PC_PLUS_4);
        if (!id_ex.valid) {
            ex_mem.valid = false;
            return;
        }
    
        //Refresh src‐mux inputs from the register filebranchBus.drive(Byte.hexToByte(computedTarget));
        for (int i = 0; i < 16; i++) {
            Word v = regFile.getReadBus(i).sample();
            src1Mux.getInputBus(i).drive(v);
            src2Mux.getInputBus(i).drive(v);
        }
    
        //Forward & select A‐input (for both ALU ops and address calc)
        int rs = id_ex.regA.byteToHex();
        Word aVal;
        if (ex_mem.valid
            && ex_mem.ctrl.regWrite.byteToHex() == 1
            && ex_mem.dest.byteToHex() == rs) {
            // forward from this cycle’s EX/MEM
            aVal = ex_mem.aluOut;
        }
        else if (mem_wb.valid
                && mem_wb.ctrl.regWrite.byteToHex() == 1
                && mem_wb.dest.byteToHex() == rs) {
            // forward from MEM/WB
            aVal = (mem_wb.ctrl.memToReg.byteToHex() == 1
                    ? mem_wb.memData
                    : mem_wb.aluOut);
        }
        else {
            // fall back to reading the register file via src1Mux
            src1Mux.update();
            aVal = (Word) src1Mux.getOutputBus().sample();
        }
        aluAinBus.drive(aVal);

        // 2) B-input / store-data forwarding (EX/MEM → MEM/WB → RF)
        boolean isStore = id_ex.ctrl.memWrite.byteToHex() == 1;
        int rt       = id_ex.regB.byteToHex();
        Word forwardedB;
        // first try EX/MEM
        if (ex_mem.valid
            && ex_mem.ctrl.regWrite.byteToHex() == 1
            && ex_mem.dest.byteToHex() == rt) {
            forwardedB = ex_mem.aluOut;
        }
        // then MEM/WB
        else if (mem_wb.valid
                && mem_wb.ctrl.regWrite.byteToHex() == 1
                && mem_wb.dest.byteToHex() == rt) {
            forwardedB = (mem_wb.ctrl.memToReg.byteToHex() == 1
                        ? mem_wb.memData
                        : mem_wb.aluOut);
        }
        // otherwise from the register file
        else {
            src2Mux.update();
            forwardedB = (Word) src2Mux.getOutputBus().sample();
        }

        Word aluB, storeData;
        if (isStore) {
            // address = imm, data = forwarded regB
            aluB      = id_ex.imm;
            storeData = forwardedB;
        } else {
            // ALU ops pick imm vs forwarded regB
            aluB      = (id_ex.ctrl.aluSrc.byteToHex() == 1
                        ? id_ex.imm
                        : forwardedB);
            storeData = forwardedB;  // not used on non-stores
        }

        // Drive the rest of the pipeline as before
        aluBinBus.drive(aluB);
        alu.compute();
        Word aluResult = aluOutBus.sample();
        Byte    target    = Byte.hexToByte(imm8);
        boolean zeroFlag = aluZeroBus.sample(); 
        boolean overflowFlag = aluOverflowBus.sample();

        //Snapshot current EX/MEM for forwarding
        lastExMem.ctrl    = ex_mem.ctrl;
        lastExMem.dest    = ex_mem.dest;
        lastExMem.valid   = ex_mem.valid;
        lastExMem.aluOut  = ex_mem.aluOut;
        lastExMem.regBval = ex_mem.regBval;
        lastExMem.aluOverflow = overflowFlag;
    
        //Latch new EX/MEM values
        ex_mem.ctrl    = new ControlSignals(id_ex.ctrl);
        ex_mem.dest    = id_ex.dest;
        ex_mem.valid   = true;
        ex_mem.aluOut  = aluResult;
        ex_mem.regBval = storeData;
        ex_mem.pc      = id_ex.pc;
        ex_mem.instr   = id_ex.instr;
        ex_mem.aluOverflow = overflowFlag;
        
        //Branch logic
        if (overflowFlag) {
            // point to your overflow vector, e.g. 0xFE
            branchBus.drive(Byte.hexToByte(0xFE));
            pcSelBus.drive(PCSel.OVERFLOW);
            id_ex.flush = true;
        }
        else if (isBranch && zeroFlag) {
            branchBus.drive(target);
            pcSelBus.drive(PCSel.BRANCH_ZERO);
            id_ex.flush = true;
        }
        else {
            // normal fall-through
            branchBus.drive(Byte.hexToByte(0));
            pcSelBus.drive(PCSel.PC_PLUS_4);
        }
    }
    

    private void memory() {
        memReadBus .drive(ex_mem.ctrl.memRead .byteToHex() == 1);
        memWriteBus.drive(ex_mem.ctrl.memWrite.byteToHex() == 1);

        if (!ex_mem.valid) {
            mem_wb.valid = false;
            return;
        }
    
        //Clone control & dest into MEM/WB
        mem_wb.ctrl  = new ControlSignals(ex_mem.ctrl);
        mem_wb.dest  = ex_mem.dest;
        mem_wb.valid = ex_mem.valid;
        mem_wb.pc    = ex_mem.pc;
        mem_wb.instr = ex_mem.instr;
        mem_wb.aluOverflow = ex_mem.aluOverflow;
    
        //Drive address & data signals
        boolean doRead  = ex_mem.ctrl.memRead .byteToHex() == 1;
        boolean doWrite = ex_mem.ctrl.memWrite.byteToHex() == 1;
        memReadBus .drive(doRead);
        memWriteBus.drive(doWrite);
        Byte dataAddr = new Byte(0);
        if (doWrite) {
            dataAddr = new Byte(ex_mem.aluOut.wordToHex() & 0xFF);
            dataAddrBus.drive(dataAddr);
            dataBus.drive(ex_mem.regBval);
        }
    
        //Perform memory cycle
        dataMem.cycle();

        //Out bus for display (only outputs the 0xFF address)
        outBus.drive(dataMem.sendToOutBus());
        if (dataAddr.byteToHex() == 0xFF) {
            outputTriggered = true;
        }
        else {
            outputTriggered = false;
        }
        
    
        //Capture this instruction’s data and ALU result
        mem_wb.memData = doRead
                       ? dataBus.sample()
                       : new Word(0);
        mem_wb.aluOut  = ex_mem.aluOut;
    }
    
    private void writeBack() {
        regWriteBus.drive(mem_wb.ctrl.regWrite.byteToHex()==1);

        if (mem_wb.ctrl.memToReg.byteToHex() == 1) {
            memToRegSel.drive(Byte.hexToByte(1));
        } else {
            memToRegSel.drive(Byte.hexToByte(0));
        }
        if (mem_wb.valid && mem_wb.ctrl.halt.byteToHex() == 1) {
                halted = true;
                return;
        }

        if (!mem_wb.valid || mem_wb.ctrl.regWrite.byteToHex() == 0) {
            return;
        }
    
        //Grab the saved values from MEM/WB
        Word aluVal = mem_wb.aluOut;
        Word memVal = mem_wb.memData;

        //Drive the write-back MUX with those saved values
        wbMux.getInputBus(0).drive(aluVal);
        wbMux.getInputBus(1).drive(memVal);
        wbMux.update();
    
        //Sample the selected value and write to RegFile
        Word val     = (Word) wbMux.getOutputBus().sample();
        int  destIdx = mem_wb.dest.byteToHex();
        if (destIdx != 0) {
            regFile.write(destIdx, val);
        }
    }


    public void loadProgram(Word[] prog) {
        for (int i = 0; i < prog.length; i++) {
            instrMem.write(Byte.hexToByte(i), prog[i]);
        }
    }
    public void loadData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            dataMem.write(Byte.hexToByte(i),
                          new Word(data[i]));
        }
    }
    public void printDataMemory() {
        dataMem.printChangedMemory();
    }

    public void printInstructionMemory() {
        instrMem.printMemory();
    }

    public void printRegisters() {
        regFile.printRegisters();
    }

    public Map<String, Object> snapshotBuses() {
        Map<String, Object> m = new LinkedHashMap<>();

        //PC & fetch
        m.put("pcBus",       pcBus.sample());
        m.put("branchBus",    branchBus.sample());
        m.put("pcSelBus",     pcSelBus.sample());

        //Instruction fetch
        m.put("instrAddrBus", instrAddrBus.sample());
        m.put("instrBus",     instrBus.sample());

        //Decode/ID stage selects
        m.put("src1SelBus",   src1SelBus.sample());
        m.put("src2SelBus",   src2SelBus.sample());
        m.put("immBus",       immBus.sample());

        //Control signals
        m.put("aluOpBus",     aluOpBus.sample());
        m.put("aluSrcBus",    aluSrcBus.sample());
        m.put("aluOverflowBus",  aluOverflowBus.sample());
        m.put("aluCarryBus",  aluCarryBus.sample());
        m.put("aluNegativeBus",aluNegativeBus.sample());
        m.put("memReadBus",   memReadBus.sample());
        m.put("memWriteBus",  memWriteBus.sample());
        m.put("memToRegSel",  memToRegSel.sample());
        m.put("regWriteBus",  regWriteBus.sample());
        m.put("branchEnBus",  branchEnBus.sample());
        m.put("writeBackBus", writeBackBus.sample());

        //Execute stage
        m.put("aluAinBus",    aluAinBus.sample());
        m.put("aluBinBus",    aluBinBus.sample());
        m.put("aluOutBus",    aluOutBus.sample());
        m.put("aluZeroBus",   aluZeroBus.sample());

        //Memory stage
        m.put("dataAddrBus",  dataAddrBus.sample());
        m.put("dataBus",      dataBus.sample());

        m.put("haltBus",     haltBus.sample());
        m.put("outBus",      outBus.sample());

        m.put("ctrlBranch", id_ex.ctrl.branch.byteToHex());
        return m;
    }
    public Map<Integer,Word> dumpRegisters() {
        return regFile.readAll();    //this is for the GUI to display registers
      }
      public Map<Integer,Word> dumpDataMemory() {
        return dataMem.readAll();      //this is for the GUI to display data memory
    }

    public boolean isOutputTriggered() {
        return outputTriggered;
    }

    public IfId getIfId() {
        return if_id;
    }

    public IdEx getIdEx() {
        return id_ex;
    }

    public ExMem getExMem() {
        return ex_mem;
    }

    public MemWb getMemWb() {
        return mem_wb;
    }
}
