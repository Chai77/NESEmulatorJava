package nes_emulator.nes.components.cpu;

public class ValueWithMemory {
    public int value;
    public int address;

    public ValueWithMemory(int value, int address) {
        this.value = value;
        this.address = address;
    }

    public ValueWithMemory() {
        this.value = 0;
        this.address = 0;
    }
}