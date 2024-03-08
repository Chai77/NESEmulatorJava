package nes_emulator.nes.components.memory;

public abstract class Memory {
    protected int memorySize;
    protected int[] memoryBytes;

    public int readByte(int addr) {
        return memoryBytes[addr % memorySize] & 0xFF;
    }

    public void writeByte(int addr, int val) {
        memoryBytes[addr % memorySize] = val & 0xFF;
    }
}
