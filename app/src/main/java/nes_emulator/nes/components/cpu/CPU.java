package nes_emulator.nes.components.cpu;

import nes_emulator.nes.components.bus.MainBus;
import nes_emulator.nes.components.memory.CPUMemory;

public class CPU {

    private CPURegisterState registers;
    private CPUStack cpuStack;
    private CPUMemory cpuMemory;
    private MainBus mainBus;

    public CPUMemory getCpuMemory() {
        return cpuMemory;
    }

    public CPUStack getCpuStack() {
        return cpuStack;
    }

    public MainBus getMainBus() {
        return mainBus;
    }

    public CPURegisterState getRegisters() {
        return registers;
    }

    public CPU() {
        cpuMemory = new CPUMemory();
        mainBus = new MainBus(this);
        cpuStack = new CPUStack(this);
        registers = new CPURegisterState();
    }

    public void executeNextCmd() throws Exception {
        CPUInstruction nextInstruction = new CPUInstruction(this);
        nextInstruction.executeCommand();
    }

}
