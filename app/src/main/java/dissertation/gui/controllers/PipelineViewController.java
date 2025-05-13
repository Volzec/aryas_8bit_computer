package dissertation.gui.controllers;

import java.io.IOException;
import java.util.*;

import dissertation.IfId;
import dissertation.IdEx;
import dissertation.ExMem;
import dissertation.MemWb;

import dissertation.Main;
import dissertation.Main.PipelineStage;
import dissertation.Word;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.stage.Stage;
import dissertation.Compiler;

public class PipelineViewController {
    @FXML private ListView<String> stepList;
    @FXML private TableView<Row>   busTable;
    @FXML private TableColumn<Row,String> colName, colValue;
    @FXML private Button stepButton, diagramButton, closeButton;

    private final ObservableList<String> steps    = FXCollections.observableArrayList();
    private final List<Map<String,String>> history = new ArrayList<>();

    private final List<Map<Integer, Word>> regHistory = new ArrayList<>();
    private final List<Map<Integer, Word>> memHistory = new ArrayList<>();
    private final List<Map<String,Object>> pipelineHistory = new ArrayList<>();
    private Main machine = new Main();

    private boolean manualMode;
    private int     stageIndex = 0;
    private static final PipelineStage[] ORDER = PipelineStage.values();

    @FXML
    private void t(ContextMenuEvent event) {
        System.out.println("Context menu invoked at " + event.getScreenX() + "," + event.getScreenY());
    }

    //Initialize table and wiring. Call this right after load()
    public void init(boolean manualMode) {
        this.manualMode = manualMode;

        //ListView of steps
        stepList.setItems(steps);
        stepList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
        
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    setOpacity(1.0);
                } else {
                    setText(item);
                    if (item.endsWith("HALT")) {
                        setDisable(true);
                        setOpacity(0.5);
                    } else {
                        setDisable(false);
                        setOpacity(1.0);
                    }
                }
            }
        });
        stepList.getSelectionModel()
        .selectedIndexProperty()
        .addListener((obs, oldIdx, newIdx) -> {
            if (newIdx.intValue() >= 0) 
                showBusSnapshot(newIdx.intValue());
        });
        stepList.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 2) {
                int idx = stepList.getSelectionModel().getSelectedIndex();
                if (idx >= 0) {
                    showRegsAndDataDialog();
                }
            }
        });
        //Table columns
        colName .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().busName));
        colValue.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().value));

        stepList.getSelectionModel()
            .selectedIndexProperty()
            .addListener((obs, oldIdx, newIdx) -> {
                int idx = newIdx.intValue();
                // guard against out-of-range
                if (idx >= 0 && idx < history.size()) {
                    showBusSnapshot(idx);
                }
            });
            
        diagramButton.disableProperty().bind(
            Bindings.createBooleanBinding(
                () -> {
                int idx = stepList.getSelectionModel().getSelectedIndex();
                return idx < 0 || idx + 1 >= history.size();
                },
                stepList.getSelectionModel().selectedIndexProperty()
            )
            );
        
       diagramButton.setOnAction(e -> {
            int idx = stepList.getSelectionModel().getSelectedIndex();
            if (idx < 0 || idx+1 >= history.size()) {
                return;
            }
            String selectedStage = stepList.getSelectionModel().getSelectedItem();
            Map<String,String> snap     = history.get(idx);
            Map<String,Object> stageRegs = pipelineHistory.get(idx);

            try {
                FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/dissertation/gui/CpuDiagramView.fxml")
                );
                Parent diagramRoot = loader.load();

                CpuDiagramController diagCtrl = loader.getController();
                diagCtrl.setPipelineRegisters(stageRegs);
                diagCtrl.setStage(selectedStage);
                diagCtrl.setSnapshot(snap);
                diagCtrl.highlightStage();

                Stage st = new Stage();
                st.setTitle("CPU Diagram — " + selectedStage);
                st.setScene(new Scene(diagramRoot));
                st.show();
            } catch (IOException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Cannot load CPU diagram").showAndWait();
            }
        });
        stepButton.setOnAction(e -> onStep());
        closeButton.setOnAction(e ->
            ((Stage)closeButton.getScene().getWindow()).close());
    }

    private void showRegsAndDataDialog() {
        //grab the latest registers & data memory from Main
        int idx = stepList.getSelectionModel().getSelectedIndex();
        Map<Integer,Word> regs = regHistory.get(idx);
        Map<Integer,Word> mem  = memHistory.get(idx);

        //build two TableViews
        TableView<RegRow>   regTable = new TableView<>();
        TableColumn<RegRow,Integer> colRIdx = new TableColumn<>("Register No.");
        TableColumn<RegRow,String>  colRVal = new TableColumn<>("Value");
        TableColumn<RegRow,String>  colRSignedVal = new TableColumn<>("Signed Value");
        regTable.setRowFactory(tv -> {
            TableRow<RegRow> row = new TableRow<>();
            row.setStyle("-fx-padding: 0;");
            return row;
        });
        colRIdx.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().idx).asObject());
        colRVal.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().value));
        colRSignedVal.setCellValueFactory(c -> {
            int value = Integer.parseInt(c.getValue().value.substring(2), 16);
            String signedValue = String.valueOf((byte) value);
            return new SimpleStringProperty(signedValue);
        });
        regTable.getColumns().addAll(colRIdx, colRVal, colRSignedVal);
        regTable.setItems(FXCollections.observableArrayList());
        regs.forEach((i,b) -> regTable.getItems().add(
            new RegRow(i, String.format("0x%02X", b.wordToHex()))
        ));

        TableView<MemRow> memTable = new TableView<>();
        TableColumn<MemRow,Integer> colMAddr = new TableColumn<>("Address");
        TableColumn<MemRow,String>  colMVal  = new TableColumn<>("Byte");
        TableColumn<MemRow,String>  colMSignedVal = new TableColumn<>("Signed Value");
        colMAddr.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().addr).asObject());
        colMVal .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().value));
        colMSignedVal.setCellValueFactory(c -> {
            int value = Integer.parseInt(c.getValue().value.substring(2), 16);
            String signedValue = String.valueOf((byte) value);
            return new SimpleStringProperty(signedValue);
        });
        memTable.getColumns().addAll(colMAddr, colMVal, colMSignedVal);
        memTable.setItems(FXCollections.observableArrayList());
        memTable.setRowFactory(tv -> {
            TableRow<MemRow> row = new TableRow<>();
            row.setStyle("-fx-padding: 0;");
            return row;
        });
        mem.forEach((a,b) -> memTable.getItems().add(
            new MemRow(a, String.format("0x%02X", b.wordToHex()))
        ));

        //lay them out in a dialog
        SplitPane sp = new SplitPane();
        sp.getItems().addAll(regTable, memTable);
        regTable.setMinWidth(200);
        memTable.setMinWidth(200);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Registers & Data Memory");
        dlg.getDialogPane().setContent(sp);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    public static class RegRow {
        public final int    idx;
        public final String value;
        public RegRow(int idx, String value) {
        this.idx = idx; this.value = value;
        }
    }
    public static class MemRow {
        public final int    addr;
        public final String value;
        public MemRow(int addr, String value) {
        this.addr = addr; this.value = value;
        }
    }

    @SuppressWarnings("static-access")
    private void onStep() {
        if (machine.isHalted()) return;
    
        if (manualMode) {
            PipelineStage stage = ORDER[stageIndex];
            recordStep(stage.name());
            stageIndex = (stageIndex + 1) % ORDER.length;
        } else {
            for (PipelineStage s : ORDER) {
                recordStep(s.name());
                if (machine.isHalted()) break;
            }
        }
    
        Map<String,String> snapshot = history.get(stepList.getSelectionModel().getSelectedIndex());
        boolean haltedBool = machine.isHalted();
        
        if (haltedBool) {
            history.add(new LinkedHashMap<>(snapshot));
            steps.add(String.format("%d: HALT", history.size() - 1));
        
            regHistory.add(regHistory.get(regHistory.size() - 1));
            memHistory.add(memHistory.get(memHistory.size() - 1));
        
            stepList.getSelectionModel().select(history.size() - 1);
            stepButton.setDisable(true);
        }
    }

    @SuppressWarnings("static-access")
    private void recordStep(String label) {
        machine.pipelineRun(PipelineStage.valueOf(label));

        Map<String,Object> raw = Main.getBusValues();

        Map<String,String> snap = new LinkedHashMap<>();
        raw.forEach((k,v) -> {
            String val = v instanceof dissertation.Byte b
                       ? String.format("0x%02X", b.byteToHex())
                       : v instanceof dissertation.Word w
                         ? String.format("0x%04X", w.wordToHex())
                         : v.toString();
            snap.put(k, val);
        });
        //store history and update ListView
        history.add(snap);

        Map<String,Object> liveRegs  = Main.getPipelineRegisters();
        Map<String,Object> cloneRegs = clonePipelineRegisters(liveRegs);
        pipelineHistory.add(cloneRegs);

       Object regForStage;
        switch(label) {
        case "IF":   regForStage = cloneRegs.get("IfId");  break;
        case "ID":   regForStage = cloneRegs.get("IdEx");  break;
        case "EX":   regForStage = cloneRegs.get("ExMem"); break;
        case "MEM":
        case "WB":   regForStage = cloneRegs.get("MemWb"); break;
        default:     regForStage = null;
        }

        //Extract the machine-word int (0–0xFFFF)
        int machineWord = 0;
        if (regForStage instanceof IfId) {
            machineWord = ((IfId) regForStage).getInstr().wordToHex();
        } else if (regForStage instanceof IdEx) {
            machineWord = ((IdEx) regForStage).getInstr().wordToHex();
        } else if (regForStage instanceof ExMem) {
            machineWord = ((ExMem) regForStage).getInstr().wordToHex();
        } else if (regForStage instanceof MemWb) {
            machineWord = ((MemWb) regForStage).getInstr().wordToHex();
        }

        //decompile to assembly
        String asmMnemonic = "";
        try {
            asmMnemonic = Compiler.deCompileSingleInstruction(machineWord);
        } catch (Exception e) {
            asmMnemonic = "";
        }

        // Now build your ListView entry
        int stepNum  = history.size() - 1;
        int cycleNum = stepNum / ORDER.length;
        String entry = String.format("C%d: %s", cycleNum, label);
        if (asmMnemonic != null
            && !asmMnemonic.isBlank()
            && !asmMnemonic.startsWith("INVALID")) {
            entry += "   " + asmMnemonic;
        }

        steps.add(entry);

        regHistory.add(machine.getRegisterValues());
        memHistory.add(machine.getDataMemoryValues());

        stepList.getSelectionModel().select(stepNum);
    }

    private void showBusSnapshot(int idx) {
        if (idx < 0 || idx >= history.size()) {
            return;
        }
        busTable.getItems().clear();
        Map<String,String> snap = history.get(idx);
        snap.forEach((k,v) -> busTable.getItems().add(new Row(k,v)));
    }

    public static class Row {
        public final String busName, value;
        public Row(String busName, String value) {
            this.busName = busName;
            this.value   = value;
        }
    }

    private Map<String,Object> clonePipelineRegisters(Map<String,Object> regs) {
        Map<String,Object> copy = new LinkedHashMap<>();
        //IF/ID
        IfId  ifid = (IfId)  regs.get("IfId");
        copy.put("IfId",  new IfId(ifid));      // copy constructor
        //ID/EX
        IdEx  idex = (IdEx)  regs.get("IdEx");
        copy.put("IdEx",  new IdEx(idex));
        //EX/MEM
        ExMem exm = (ExMem) regs.get("ExMem");
        copy.put("ExMem", new ExMem(exm));
        //MEM/WB
        MemWb mw = (MemWb) regs.get("MemWb");
        copy.put("MemWb", new MemWb(mw));
        return copy;
    }
}