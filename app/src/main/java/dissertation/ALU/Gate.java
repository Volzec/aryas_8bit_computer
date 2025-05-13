package dissertation.ALU;

import java.util.ArrayList;
import java.util.List;
import dissertation.*;
import dissertation.Byte;


//Gate-level simulation framework. Each Gate operates on one-bit inputs and produces one-bit output.
public interface Gate {
    void setInputs(boolean[] inputs);
    boolean getOutput();
}