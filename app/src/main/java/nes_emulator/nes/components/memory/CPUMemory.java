package nes_emulator.nes.components.memory;

import java.util.ArrayList;

public class CPUMemory extends Memory {
    public CPUMemory() {
//        memorySize = 0x800;
        memorySize = 0x10000;

        memoryBytes = new int[memorySize];
    }

    public void initialize(int[][] ram) {
        for (int i = 0; i < memoryBytes.length; i++) {
            memoryBytes[i] = 0x100;
        }
        for (int i = 0; i < ram.length; i++) {
            memoryBytes[ram[i][0]] = ram[i][1];
        }
    }

    public ArrayList<int[]> getMemoryArr() {
        ArrayList<int[]> result = new ArrayList<>();
        for (int i = 0; i < memoryBytes.length; i++) {
            if (memoryBytes[i] >= 256) {
                continue;
            }
            result.add(new int[]{i, memoryBytes[i]});
        }
        return result;
    }
}
