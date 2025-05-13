package dissertation.gui.controllers;

import dissertation.*;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.Node;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class CpuDiagramController implements Initializable {

    @FXML private Line ALUMEMthrough1;
    @FXML private Line ALUMEMthrough2;
    @FXML private Line ALUMEMthrough3;
    @FXML private Line ALUWB;
    @FXML private Line ALUin;
    @FXML private Line ALUfunc;
    @FXML private Line ALUout1;
    @FXML private Line ALUout2;
    @FXML private Line ALUzero;
    @FXML private Line SRC1;
    @FXML private Line SRC2_1;
    @FXML private Line SRC2_2;
    @FXML private Line SRC2_3;
    @FXML private Line WBbus1;
    @FXML private Line WBbus2;
    @FXML private Line WBbus3;
    @FXML private Line WBbus4;
    @FXML private Line WBbus5;
    @FXML private Line wbMuxOut;
    @FXML private Line branchEn;
    @FXML private Line branchbus1;
    @FXML private Line branchbus2;
    @FXML private Line branchbus3;
    @FXML private Line branchbus4;
    @FXML private Line ctrlSgnl;
    @FXML private Line ctrlSgnlThrough1;
    @FXML private Line ctrlSgnlThrough2;
    @FXML private Line destSgnl;
    @FXML private Line destSgnlThrough1;
    @FXML private Line destSgnlThrough2;
    @FXML private Line immBus;
    @FXML private Line immBus2;
    @FXML private Line immBus3;
    @FXML private Line inAddrBus1;
    @FXML private Line inAddrBus11;
    @FXML private Line inAddrBus2;
    @FXML private Line inBus;
    @FXML private Line inDataBus;
    @FXML private Line memData;
    @FXML private Line memRead;
    @FXML private Line memToReg1;
    @FXML private Line memToReg2;
    @FXML private Line memWrite;
    @FXML private Line dataBus;
    @FXML private Line op1;
    @FXML private Line op2;
    @FXML private Line outputBus1;
    @FXML private Line outputBus2;
    @FXML private Line outputBus3;
    @FXML private Line pcBus;
    @FXML private Line pcThrough;
    @FXML private Line regBThrough;
    @FXML private Line regBval;
    @FXML private Line registerBus1;
    @FXML private Line registerBus2;
    @FXML private Line registerBus3;
    @FXML private Line registerBus4;
    @FXML private Line registerBus5;
    @FXML private Line destSgnlWB1;
    @FXML private Line destSgnlWB2;

    @FXML private TextArea instruction;

    @FXML private TextArea output;

    @FXML private HBox ifid;

    @FXML private HBox idex;

    @FXML private HBox exmem;

    @FXML private HBox memwb;

    @FXML private Parent root;

    @FXML private Map<String,String> snapshot;

    @FXML private Map<String,Object> pipelineRegs;

    private String stage;

    private static final Color INACTIVE_COLOR = Color.web("#4d4d4d");

    public void setStage(String stage) {
        this.stage = stage;
    }

    public void setPipelineRegisters(Map<String,Object> regs) {
        this.pipelineRegs = regs;
    }

    @Override
    public void initialize(URL loc, ResourceBundle res) {
        ifid .setPickOnBounds(true);
        idex .setPickOnBounds(true);
        exmem.setPickOnBounds(true);
        memwb.setPickOnBounds(true);

        attachContextMenuHandler(root);
        setupPipelineContextMenu(ifid,  "IfId");
        setupPipelineContextMenu(idex,  "IdEx");
        setupPipelineContextMenu(exmem, "ExMem");
        setupPipelineContextMenu(memwb, "MemWb");
    }

    private void setupPipelineContextMenu(HBox box, String stageKey) {

        box.addEventHandler(MouseEvent.MOUSE_CLICKED, evt -> {
            //only pop up on right–click (or change to PRIMARY for left–click)
            if (evt.getButton() != MouseButton.PRIMARY) return;

            //grab the register object from the map you were already handed
            Object reg = pipelineRegs.get(stageKey);
            if (reg == null) return;

            ContextMenu menu = new ContextMenu();
            //reflectively or manually unpack fields for each stage
            switch(stageKey) {
                case "IfId":
                    IfId    ifidReg = (IfId)    reg;
                    int instruction = ifidReg.getInstr().wordToHex();
                    menu.getItems().addAll(
                    new MenuItem("pc = "   + ifidReg.getPc().byteToHexString()),
                    new MenuItem("instr = "+ ifidReg.getInstr().wordToHexString()),
                    new MenuItem("task =" + Compiler.deCompileSingleInstruction(instruction)),
                    new MenuItem("valid = "  + ifidReg.isValid()),
                    new MenuItem("flush = "  + ifidReg.isFlush())
                    );
                    break;

                case "IdEx":
                    IdEx    idexReg = (IdEx)    reg;
                    instruction = idexReg.getInstr().wordToHex();
                    menu.getItems().addAll(
                    new MenuItem("pc = "      + idexReg.getPc().byteToHexString()),
                    new MenuItem("instr = "   + idexReg.getInstr().wordToHexString()),
                    new MenuItem("task =" + Compiler.deCompileSingleInstruction(instruction)),
                    new MenuItem("valid = "    + idexReg.isValid()),
                    new MenuItem("dest = "     + idexReg.getDest().byteToHexString()),
                    new MenuItem("regA = " + idexReg.getRegA().byteToHexString()),
                    new MenuItem("regB = " + idexReg.getRegB().byteToHexString()),
                    new MenuItem("imm = "   + idexReg.getImm().wordToHex()),
                    new MenuItem("aluOp = "    + idexReg.getCtrl().aluOp.byteToHexString()),
                    new MenuItem("aluSrc = "   + idexReg.getCtrl().aluSrc.byteToHexString()),
                    new MenuItem("branch = "   + idexReg.getCtrl().branch.byteToHexString()),
                    new MenuItem("halt = "     + idexReg.getCtrl().halt.byteToHexString())
                    );
                    break;

                case "ExMem":
                    ExMem   exmemReg = (ExMem)   reg;
                    instruction = exmemReg.getInstr().wordToHex();
                    menu.getItems().addAll(
                    new MenuItem("pc = "      + exmemReg.getPc().byteToHexString()),
                    new MenuItem("instr = "   + exmemReg.getInstr().wordToHexString()),
                    new MenuItem("task =" + Compiler.deCompileSingleInstruction(instruction)),
                    new MenuItem("valid = "   + exmemReg.isValid()),
                    new MenuItem("aluOut= " + exmemReg.getAluOut().wordToHexString()),
                    new MenuItem("regB= "   + exmemReg.getRegBval().wordToHexString()),
                    new MenuItem("dest= "    + exmemReg.getDest().byteToHexString()),
                    new MenuItem("memRead= "  + exmemReg.getCtrl().memRead.byteToHexString()),
                    new MenuItem("memWrite= " + exmemReg.getCtrl().memWrite.byteToHexString())
                    );
                    break;

                case "MemWb":
                    MemWb   memwbReg = (MemWb)   reg;
                    instruction = memwbReg.getInstr().wordToHex();
                    menu.getItems().addAll(
                    new MenuItem("pc = "      + memwbReg.getPc().byteToHexString()),
                    new MenuItem("instr = "   + memwbReg.getInstr().wordToHexString()),
                    new MenuItem("task =" + Compiler.deCompileSingleInstruction(instruction)),
                    new MenuItem("valid = "     + memwbReg.isValid()),
                    new MenuItem("memData= "   + memwbReg.getMemData().wordToHexString()),
                    new MenuItem("aluOut= "    + memwbReg.getAluOut().wordToHexString()),
                    new MenuItem("dest= "       + memwbReg.getDest().byteToHexString()),
                    new MenuItem("memToReg= "    + memwbReg.getCtrl().memToReg.byteToHexString()),
                    new MenuItem("regWrite= "    + memwbReg.getCtrl().regWrite.byteToHexString())
                    );
                    break;
            }

            //show it at the mouse cursor
            menu.show(box, evt.getScreenX(), evt.getScreenY());
            evt.consume();
        });
    }

    private void highlight(String... fxIds) {
        for (String id : fxIds) {
            Line line = (Line) root.lookup("#" + id);
            if (line != null) {
                line.setStroke(Color.RED);
                line.setStrokeWidth(1);
            }
        }
    }

    public void setInstructionBox(String instruction) {
        this.instruction.setText(instruction);
    }

    public void setOutputBox(String output) {
        this.output.setText(output);
    }

    public void highlightStage() {
        String inst = snapshot.getOrDefault("instrBus", "0000");
        String out = snapshot.getOrDefault("outBus", "0000");
        setInstructionBox(inst);
        setOutputBox(out);
        setAllLinesColor(INACTIVE_COLOR);
        String key = stage;
        if (stage.contains(":")) {
            String after = stage.substring(stage.indexOf(':') + 1).trim();
            key = after.split("\\s+")[0];
        }
        key = key.toUpperCase();
        highlight("outputBus1", "outputBus2", "outputBus3");

        switch (key) {
            case "IF":
                highlight("pcBus", "inAddrBus1", "inAddrBus2", "inDataBus1", "inDataBus2");
                break;
    
            case "ID":
                highlight("inBus", "pcThrough", "ALUin", "destSgnl", "ctrlSgnl");
                break;
    
            case "EX":
                highlight("ALUfunc","ALUout1", "ALUout2", "op1", "op2");
                highlight("immBus", "immBus2", "immBus3", "SRC1", "SRC2_1", "SRC2_2", "SRC2_3");
                highlight("registerBus1", "registerBus2", "registerBus3", "registerBus4", "registerBus5", "ctrlSgnlThrough1", "destSgnlThrough1", "regBThrough");

                boolean branchEn = "1".equals(snapshot.getOrDefault("ctrlBranch", "0"));
                boolean aluZero  = "true".equalsIgnoreCase(snapshot.getOrDefault("aluZeroBus", "false"));
                if (branchEn) {
                    highlight("branchEn");
                }
                if (aluZero) {
                    highlight("ALUzero");
                }
                if (branchEn && aluZero) {
                    highlight("branchbus1", "branchbus2", "branchbus3", "branchbus4");
                }
                break;
    
            case "MEM":
                boolean memRead  = "true".equals(snapshot.getOrDefault("memReadBus", "0"));
                boolean memWrite = "true".equals(snapshot.getOrDefault("memWriteBus", "0"));
                highlight("ALUMEMthrough1", "ALUMEMthrough2", "ALUMEMthrough3", "regBval");
                highlight("ctrlSgnlThrough2", "destSgnlThrough2");
                if (memRead) {
                    highlight("memRead", "dataBus");
                }
                if (memWrite) {
                    highlight("memWrite");
                }
                break;
    
            case "WB":
                highlight("memData", "ALUWB", "wbMuxOut", "WBbus1", "WBbus2", "WBbus3", "WBbus4", "WBbus5", "destSgnlWB1", "destSgnlWB2");
                boolean memToReg = "1".equals(snapshot.getOrDefault("memToRegSel", "0"));
                if (memToReg) {
                    highlight("memToReg1", "memToReg2");
                }
                break;
    
            default:
                //nothing
        }
    }

    private void setAllLinesColor(Color c) { setAllLinesColor(root, c); }

private void setAllLinesColor(Parent parent, Color c) {
    for (Node n : parent.getChildrenUnmodifiable()) {
        if (n instanceof Line) {
            ((Line)n).setStroke(c);
            ((Line)n).setStrokeWidth(1);
        } else if (n instanceof Parent) {
            setAllLinesColor((Parent)n, c);
        }
    }
}

    private void attachContextMenuHandler(Parent parent) {
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (node instanceof Line) {
                node.setOnContextMenuRequested(this::handleContextMenu);
            } else if (node instanceof Parent) {
                attachContextMenuHandler((Parent) node);
            }
        }
    }

    private void handleContextMenu(ContextMenuEvent event) {
        event.consume();
    }

    public void setSnapshot(Map<String,String> snapshot) {
        this.snapshot = snapshot;
    }
}
