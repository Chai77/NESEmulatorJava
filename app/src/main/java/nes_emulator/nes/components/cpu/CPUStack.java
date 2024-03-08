package nes_emulator.nes.components.cpu;

import nes_emulator.nes.components.memory.CPUMemory;

public class CPUStack {

    private final CPU cpu;

    public CPUStack(CPU cpu) {
        this.cpu = cpu;
    }

    public void push(int val) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        CPUMemory cpuMemory = cpu.getCpuMemory();
        if (cpuRegisterState.S == 0x00) {
            // stack overflow occured
            System.out.println("STACK: There was a stack overflow");
        }
        int addr = 0x0100 | cpuRegisterState.S;
        cpuRegisterState.S = (cpuRegisterState.S - 1) & 0xFF;
        cpuMemory.writeByte(addr, val);
    }

    public int pop() {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        CPUMemory cpuMemory = cpu.getCpuMemory();
        if (cpuRegisterState.S == 0xFF) {
            // stack underflow occured
            System.out.println("STACK: There was a stack underflow");
        }
        cpuRegisterState.S = (cpuRegisterState.S + 1) & 0xFF;
        int addr = 0x0100 | cpuRegisterState.S;
        return cpuMemory.readByte(addr);
    }


}
