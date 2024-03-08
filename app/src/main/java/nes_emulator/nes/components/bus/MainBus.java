package nes_emulator.nes.components.bus;

import nes_emulator.nes.components.cpu.CPU;
import nes_emulator.nes.components.memory.CPUMemory;

public class MainBus {
    private CPU cpu;

    // Memory
    private CPUMemory cpuMemory;

    public MainBus(CPU cpu) {
        this.cpu = cpu;
        this.cpuMemory = cpu.getCpuMemory();
    }

    public int readByte(int addr) {
        int result = cpuMemory.readByte(addr);
        return result;
    }

//    public int readByte(int addr) {
//        int result = 0;
//        if(addr < 0x2000) {
//            result = cpuMemory.readByte(addr);
//        } else if(addr < 0x4000) {
//            // ppu registers are read
//        } else if (addr <  0x4018) {
//            // apu registers are read
//            // i/o registers are read
//        } else if (addr < 0x4020) {
//            // CPU test mode
//        } else {
//            // mapper read memory
//            result = mapper.readMainBusByte((addr - 0x4020));
//        }
//        return result;
//    }

    public void writeByte(int addr, int val) {
        cpuMemory.writeByte(addr, val);
    }

//    public void writeByte(int addr, int val) {
//        if(addr < 0x2000) {
//            cpuMemory.writeByte(addr, val);
//        } else if(addr < 0x4000) {
//            // ppu registers are written to
//        } else if (addr < 0x4018) {
//            // apu registers are written to
//            // i/o registers are written to
//        } else if (addr < 0x4020) {
//            // CPU test mode
//        } else {
//            // mapper write memory
//            mapper.writeMainBusByte(addr - 0x4020, val);
//        }
//    }
}
