package dissertation;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


import javafx.application.Application;

public class Main extends Application {
    private static PipelinedCPU cpu = new PipelinedCPU();
    private static Word[] programMemory = new Word[256];
    private static boolean isHalted = false;
    public enum PipelineStage { WB, MEM, EX, ID, IF }
    private static final List<OutBusChange> outBusChanges = new ArrayList<>();
    public static record OutBusChange(int cycle, Word value) { }
    private static Word   lastOutBusValue;
    private static Boolean started = false; 

    public Main() {
    }

    public static void resetProgram() {
        cpu = new PipelinedCPU();
        programMemory = new Word[256];
        isHalted = false;
        outBusChanges.clear();
        lastOutBusValue = null;
        started = false;
    }

    public void start(Stage primaryStage) throws Exception {
        URL resourceUrl = getClass().getResource("/dissertation/gui/MainView.fxml");
        Parent root = FXMLLoader.load(resourceUrl);
        primaryStage.setTitle("JavaFX GUI Application");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
    public static void loadProgram(String program) {
        started = true;
        PipelinedCPU cpu = new PipelinedCPU();
        String[] textprogram = formatProgram(program);
        int[] realProgram = Compiler.compileToBrookshear(textprogram);
        System.out.println("Program has been compiled to Brookshear machine code.");
        programMemory = Word.intToWords(realProgram);
        cpu.loadProgram(programMemory);
    }

    public static void startProgram() {
        outBusChanges.clear();

        lastOutBusValue = getOutBusSnapshot();
        outBusChanges.add(new OutBusChange(0, lastOutBusValue));
        
        if (isHalted) {
            return;
        }
        else {
            for (int i = 0; i < 9999999; i++) {
                cpu.loadProgram(programMemory);
                cpu.autoTick();
                //cpu.printRegisters();

                Word currentOut = getOutBusSnapshot();
                if (cpu.isOutputTriggered()) {
                    outBusChanges.add(new OutBusChange(i, currentOut));
                }
                Boolean halted = isHalted();
                if (halted){
                    System.out.println("The CPU has halted");
                    isHalted = true;
                    break;
                }
                if (i == 9999998) {
                    System.out.println("The CPU has not halted after 9999999 ticks. Please check the program for errors.");
                }
            }
        }
    }

    public static Map<String,Object> getBusValues() {
        if (cpu == null) {
            throw new IllegalStateException("CPU not created yet");
        }

        Map<String,Object> raw = cpu.snapshotBuses();

        Map<String,Object> buses = new LinkedHashMap<>();
        for (Map.Entry<String,Object> e : raw.entrySet()) {
            String   name = e.getKey();
            Object   val  = e.getValue();

            if (val instanceof dissertation.Bus<?> bus) {
                buses.put(name, bus.sample());
            } else {
                buses.put(name, val);
            }
        }
        return buses;
    }

    public static void pipelineRun(PipelineStage stage) {
        cpu.loadProgram(programMemory);
        if (cpu == null) {
            throw new IllegalStateException("CPU not created yet");
        }

        if (isHalted()) {
            return;
        }

        switch (stage) {
          case WB:  cpu.writeBackStage();  break;
          case MEM: cpu.memoryStage();     break;
          case EX:  cpu.executeStage();    break;
          case ID:  cpu.decodeStage();     break;
          case IF:  cpu.fetchStage();      break;
        }
    }
    
    public static void tickprogram() {
        if (isHalted()) return;
        cpu.loadProgram(programMemory);
        cpu.autoTick();


        if (isHalted()) {
            System.out.println("The CPU has halted");
            isHalted = true;
        }
    }

    public static Map<Integer,Word> getRegisterValues() {
        return cpu.dumpRegisters();
    }
      public static Map<Integer,Word> getDataMemoryValues() {
        return cpu.dumpDataMemory();
    }

    public static void getIntructionMemory() {
        cpu.printInstructionMemory();
    }
    
    public static boolean isHalted() {
        return cpu.isHalted();
    }

    private static String[] formatProgram(String program) {
        return program.split("\n");
    }

    private static Word getOutBusSnapshot() {
        Object raw = cpu.snapshotBuses().get("outBus");
        if (raw instanceof Bus<?> b) {
            @SuppressWarnings("unchecked")
            Bus<Word> ob = (Bus<Word>)b;
            return ob.sample();
        } else {
            return (Word) raw;
        }
    }

    public static List<OutBusChange> getOutBusChanges() {
        return new ArrayList<>(outBusChanges);
    }

    public boolean getStarted() {
        return started;
    }

    public static Map<String,Object> getPipelineRegisters() {
        Map<String,Object> regs = new LinkedHashMap<>();
        regs.put("IfId", cpu.getIfId());
        regs.put("IdEx", cpu.getIdEx());
        regs.put("ExMem", cpu.getExMem());
        regs.put("MemWb", cpu.getMemWb());
        return regs;
    }
}