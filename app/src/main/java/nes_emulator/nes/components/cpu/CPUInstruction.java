package nes_emulator.nes.components.cpu;

import nes_emulator.nes.components.bus.MainBus;

public class CPUInstruction {
    public int numCycles;
    public AddressingMode addressingMode;
    public String opcodeAssembly;
    public int opcode;

    private ValueWithMemory addressedValue;
    private CPU cpu;

    enum AddressingMode {
        IMPLIED("implied", 1),
        IMMEDIATE("immediate", 2),
        ABSOLUTE("absolute", 3),
        ZEROPAGE("zero-page", 2),
        INDEXED_ABSOLUTE_X("absolute, X-indexed", 3),
        INDEXED_ABSOLUTE_Y("absolute, Y-indexed", 3),
        INDEXED_ZEROPAGE_X("zeropage, X-indexed", 2),
        INDEXED_ZEROPAGE_Y("zeropage, Y-indexed", 2),
        INDIRECT("indirect", 3),
        PRE_INDEXED_INDIRECT("X-indexed, indirect", 2),
        POST_INDEXED_INDIRECT("indirect, Y-indexed", 2),
        RELATIVE("relative", 2),
        ACCUMULATOR("accumulator", 1);

        private String addressingModeName;
        private int numBytes;

        AddressingMode(String addressingModeName, int numBytes) {
            this.addressingModeName = addressingModeName;
            this.numBytes = numBytes;
        }

        public int getNumBytes() {
            return numBytes;
        }

        @Override
        public String toString() {
            return addressingModeName;
        }
    }

    public CPUInstruction(CPU cpu) throws Exception {
        this.cpu = cpu;

        MainBus mainBus = cpu.getMainBus();
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        opcode = mainBus.readByte(cpuRegisterState.PC);
        findCommand();

        addressedValue = getValueWithAddressingMode(mainBus, cpuRegisterState);
    }

    // NOTE: we are reading in the first cycle so there is a chance that the PPU registers update when we actually need to read at the end
    public ValueWithMemory getValueWithAddressingMode(MainBus mainBus, CPURegisterState cpuRegisterState) {
        // TODO: get the full assembly in this method
        int currPC = cpuRegisterState.PC;
        if (addressingMode == AddressingMode.IMPLIED) {
            return new ValueWithMemory();
        }
        if (addressingMode == AddressingMode.IMMEDIATE) {
            return new ValueWithMemory(mainBus.readByte(currPC + 1), currPC + 1);
        }
        if (addressingMode == AddressingMode.ABSOLUTE) {
            int first = mainBus.readByte(currPC + 1);
            int second = (mainBus.readByte(currPC + 2) << 8) & 0xFF00;
            int final_addr = first + second;
            return new ValueWithMemory(mainBus.readByte(final_addr), final_addr);
        }
        if (addressingMode == AddressingMode.ZEROPAGE) {
            int first = mainBus.readByte(currPC + 1);
            return new ValueWithMemory(mainBus.readByte(first), first);
        }
        if (addressingMode == AddressingMode.INDEXED_ABSOLUTE_X) {
            int first = mainBus.readByte(currPC + 1);
            int second = (mainBus.readByte(currPC + 2) << 8) & 0xFF00;
            int addr = first + second;
            int final_addr = addr + cpuRegisterState.X;
            if ((final_addr & 0xFF00) != (addr & 0xFF00)) {
                // crossed a page boundary
                numCycles += 1;
            }
            return new ValueWithMemory(mainBus.readByte(final_addr), final_addr);
        }
        if (addressingMode == AddressingMode.INDEXED_ABSOLUTE_Y) {
            int first = mainBus.readByte(currPC + 1);
            int second = (mainBus.readByte(currPC + 2) << 8) & 0xFF00;
            int addr = first + second;
            int final_addr = addr + cpuRegisterState.Y;
            if ((final_addr & 0xFF00) != (addr & 0xFF00)) {
                // crossed a page boundary
                numCycles += 1;
            }
            return new ValueWithMemory(mainBus.readByte(final_addr), final_addr);
        }
        if (addressingMode == AddressingMode.INDEXED_ZEROPAGE_X) {
            int first = mainBus.readByte(currPC + 1);
            int final_addr = (first + cpuRegisterState.X) & 0xFF;
            return new ValueWithMemory(mainBus.readByte(final_addr), final_addr);
        }
        if (addressingMode == AddressingMode.INDEXED_ZEROPAGE_Y) {
            int first = mainBus.readByte(currPC + 1);
            int final_addr = (first + cpuRegisterState.Y) & 0xFF;
            return new ValueWithMemory(mainBus.readByte(final_addr), final_addr);
        }
        if (addressingMode == AddressingMode.INDIRECT) {
            int first = mainBus.readByte(currPC + 1);
            int second = (mainBus.readByte(currPC + 2) << 8) & 0xFF00;
            int first_addr = first + second;
            int first_addr_next = second + ((first + 1) & 0xFF);
            int first_res = mainBus.readByte(first_addr);
            int second_res = (mainBus.readByte(first_addr_next) << 8) & 0xFF00;
            int final_addr = first_res + second_res;
            return new ValueWithMemory(0, final_addr);
        }
        if (addressingMode == AddressingMode.PRE_INDEXED_INDIRECT) {
            int first = mainBus.readByte((currPC + 1) & 0xFFFF);
            int first_addr = (first + cpuRegisterState.X) & 0xFF;
            int first_res = mainBus.readByte(first_addr);
            int second_res = (mainBus.readByte((first_addr + 1) & 0xFF) << 8) & 0xFF00;
            int final_addr = first_res + second_res;
            return new ValueWithMemory(mainBus.readByte(final_addr), final_addr);
        }
        if (addressingMode == AddressingMode.POST_INDEXED_INDIRECT) {
            int first = mainBus.readByte((currPC + 1) & 0xFFFF);
            int first_res = mainBus.readByte(first);
            int second_res = (mainBus.readByte((first + 1) & 0xFF) << 8) & 0xFF00;
            int addr = first_res + second_res;
            int final_addr = addr + cpuRegisterState.Y;
            if ((final_addr & 0xFF00) != (addr & 0xFF00)) {
                // crossed a page boundary
                numCycles += 1;
            }
            return new ValueWithMemory(mainBus.readByte(final_addr), final_addr);
        }
        if (addressingMode == AddressingMode.RELATIVE) {
            int first = (byte)mainBus.readByte(currPC + 1);
            int final_addr = (currPC + first + 2) & 0xFFFF;
            // TODO: add a cycle if the branch is taken
            return new ValueWithMemory(0, final_addr);
        }
        if (addressingMode == AddressingMode.ACCUMULATOR) {
            return new ValueWithMemory(cpuRegisterState.A, 0);
        }
        return null;
    }

    public void findCommand() throws Exception {
        switch(opcode) {
            case 0x00:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "BRK";
                numCycles = 7;
                break;
            case 0x01:
                addressingMode = AddressingMode.PRE_INDEXED_INDIRECT;
                opcodeAssembly = "ORA";
                numCycles = 6;
                break;
            case 0x05:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "ORA";
                numCycles = 3;
                break;
            case 0x06:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "ASL";
                numCycles = 5;
                break;
            case 0x08:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "PHP";
                numCycles = 3;
                break;
            case 0x09:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "ORA";
                numCycles = 2;
                break;
            case 0x0A:
                addressingMode = AddressingMode.ACCUMULATOR;
                opcodeAssembly = "ASL";
                numCycles = 2;
                break;
            case 0x0D:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "ORA";
                numCycles = 4;
                break;
            case 0x0E:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "ASL";
                numCycles = 6;
                break;
            case 0x10:
                addressingMode = AddressingMode.RELATIVE;
                opcodeAssembly = "BPL";
                numCycles = 2;
                break;
            case 0x11:
                addressingMode = AddressingMode.POST_INDEXED_INDIRECT;
                opcodeAssembly = "ORA";
                numCycles = 5;
                break;
            case 0x15:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "ORA";
                numCycles = 4;
                break;
            case 0x16:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "ASL";
                numCycles = 6;
                break;
            case 0x18:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "CLC";
                numCycles = 2;
                break;
            case 0x19:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "ORA";
                numCycles = 4;
                break;
            case 0x1D:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "ORA";
                numCycles = 4;
                break;
            case 0x1E:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "ASL";
                numCycles = 7;
                break;
            case 0x20:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "JSR";
                numCycles = 6;
                break;
            case 0x21:
                addressingMode = AddressingMode.PRE_INDEXED_INDIRECT;
                opcodeAssembly = "AND";
                numCycles = 6;
                break;
            case 0x24:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "BIT";
                numCycles = 3;
                break;
            case 0x25:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "AND";
                numCycles = 3;
                break;
            case 0x26:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "ROL";
                numCycles = 5;
                break;
            case 0x28:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "PLP";
                numCycles = 4;
                break;
            case 0x29:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "AND";
                numCycles = 2;
                break;
            case 0x2A:
                addressingMode = AddressingMode.ACCUMULATOR;
                opcodeAssembly = "ROL";
                numCycles = 2;
                break;
            case 0x2C:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "BIT";
                numCycles = 4;
                break;
            case 0x2D:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "AND";
                numCycles = 4;
                break;
            case 0x2E:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "ROL";
                numCycles = 6;
                break;
            case 0x30:
                addressingMode = AddressingMode.RELATIVE;
                opcodeAssembly = "BMI";
                numCycles = 2;
                break;
            case 0x31:
                addressingMode = AddressingMode.POST_INDEXED_INDIRECT;
                opcodeAssembly = "AND";
                numCycles = 5;
                break;
            case 0x35:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "AND";
                numCycles = 4;
                break;
            case 0x36:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "ROL";
                numCycles = 6;
                break;
            case 0x38:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "SEC";
                numCycles = 2;
                break;
            case 0x39:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "AND";
                numCycles = 4;
                break;
            case 0x3D:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "AND";
                numCycles = 4;
                break;
            case 0x3E:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "ROL";
                numCycles = 7;
                break;
            case 0x40:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "RTI";
                numCycles = 6;
                break;
            case 0x41:
                addressingMode = AddressingMode.PRE_INDEXED_INDIRECT;
                opcodeAssembly = "EOR";
                numCycles = 6;
                break;
            case 0x45:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "EOR";
                numCycles = 3;
                break;
            case 0x46:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "LSR";
                numCycles = 5;
                break;
            case 0x48:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "PHA";
                numCycles = 3;
                break;
            case 0x49:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "EOR";
                numCycles = 2;
                break;
            case 0x4A:
                addressingMode = AddressingMode.ACCUMULATOR;
                opcodeAssembly = "LSR";
                numCycles = 2;
                break;
            case 0x4C:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "JMP";
                numCycles = 3;
                break;
            case 0x4D:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "EOR";
                numCycles = 4;
                break;
            case 0x4E:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "LSR";
                numCycles = 6;
                break;
            case 0x50:
                addressingMode = AddressingMode.RELATIVE;
                opcodeAssembly = "BVC";
                numCycles = 2;
                break;
            case 0x51:
                addressingMode = AddressingMode.POST_INDEXED_INDIRECT;
                opcodeAssembly = "EOR";
                numCycles = 5;
                break;
            case 0x55:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "EOR";
                numCycles = 4;
                break;
            case 0x56:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "LSR";
                numCycles = 6;
                break;
            case 0x58:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "CLI";
                numCycles = 2;
                break;
            case 0x59:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "EOR";
                numCycles = 4;
                break;
            case 0x5D:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "EOR";
                numCycles = 4;
                break;
            case 0x5E:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "LSR";
                numCycles = 7;
                break;
            case 0x60:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "RTS";
                numCycles = 6;
                break;
            case 0x61:
                addressingMode = AddressingMode.PRE_INDEXED_INDIRECT;
                opcodeAssembly = "ADC";
                numCycles = 6;
                break;
            case 0x65:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "ADC";
                numCycles = 3;
                break;
            case 0x66:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "ROR";
                numCycles = 5;
                break;
            case 0x68:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "PLA";
                numCycles = 4;
                break;
            case 0x69:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "ADC";
                numCycles = 2;
                break;
            case 0x6A:
                addressingMode = AddressingMode.ACCUMULATOR;
                opcodeAssembly = "ROR";
                numCycles = 2;
                break;
            case 0x6C:
                addressingMode = AddressingMode.INDIRECT;
                opcodeAssembly = "JMP";
                numCycles = 5;
                break;
            case 0x6D:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "ADC";
                numCycles = 4;
                break;
            case 0x6E:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "ROR";
                numCycles = 6;
                break;
            case 0x70:
                addressingMode = AddressingMode.RELATIVE;
                opcodeAssembly = "BVS";
                numCycles = 2;
                break;
            case 0x71:
                addressingMode = AddressingMode.POST_INDEXED_INDIRECT;
                opcodeAssembly = "ADC";
                numCycles = 5;
                break;
            case 0x75:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "ADC";
                numCycles = 4;
                break;
            case 0x76:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "ROR";
                numCycles = 6;
                break;
            case 0x78:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "SEI";
                numCycles = 2;
                break;
            case 0x79:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "ADC";
                numCycles = 4;
                break;
            case 0x7D:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "ADC";
                numCycles = 4;
                break;
            case 0x7E:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "ROR";
                numCycles = 7;
                break;
            case 0x81:
                addressingMode = AddressingMode.PRE_INDEXED_INDIRECT;
                opcodeAssembly = "STA";
                numCycles = 6;
                break;
            case 0x84:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "STY";
                numCycles = 3;
                break;
            case 0x85:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "STA";
                numCycles = 3;
                break;
            case 0x86:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "STX";
                numCycles = 3;
                break;
            case 0x88:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "DEY";
                numCycles = 2;
                break;
            case 0x8A:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "TXA";
                numCycles = 2;
                break;
            case 0x8C:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "STY";
                numCycles = 4;
                break;
            case 0x8D:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "STA";
                numCycles = 4;
                break;
            case 0x8E:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "STX";
                numCycles = 4;
                break;
            case 0x90:
                addressingMode = AddressingMode.RELATIVE;
                opcodeAssembly = "BCC";
                numCycles = 2;
                break;
            case 0x91:
                addressingMode = AddressingMode.POST_INDEXED_INDIRECT;
                opcodeAssembly = "STA";
                numCycles = 6;
                break;
            case 0x94:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "STY";
                numCycles = 4;
                break;
            case 0x95:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "STA";
                numCycles = 4;
                break;
            case 0x96:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_Y;
                opcodeAssembly = "STX";
                numCycles = 4;
                break;
            case 0x98:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "TYA";
                numCycles = 2;
                break;
            case 0x99:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "STA";
                numCycles = 5;
                break;
            case 0x9A:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "TXS";
                numCycles = 2;
                break;
            case 0x9D:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "STA";
                numCycles = 5;
                break;
            case 0xA0:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "LDY";
                numCycles = 2;
                break;
            case 0xA1:
                addressingMode = AddressingMode.PRE_INDEXED_INDIRECT;
                opcodeAssembly = "LDA";
                numCycles = 6;
                break;
            case 0xA2:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "LDX";
                numCycles = 2;
                break;
            case 0xA4:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "LDY";
                numCycles = 3;
                break;
            case 0xA5:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "LDA";
                numCycles = 3;
                break;
            case 0xA6:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "LDX";
                numCycles = 3;
                break;
            case 0xA8:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "TAY";
                numCycles = 2;
                break;
            case 0xA9:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "LDA";
                numCycles = 2;
                break;
            case 0xAA:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "TAX";
                numCycles = 2;
                break;
            case 0xAC:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "LDY";
                numCycles = 4;
                break;
            case 0xAD:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "LDA";
                numCycles = 4;
                break;
            case 0xAE:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "LDX";
                numCycles = 4;
                break;
            case 0xB0:
                addressingMode = AddressingMode.RELATIVE;
                opcodeAssembly = "BCS";
                numCycles = 2;
                break;
            case 0xB1:
                addressingMode = AddressingMode.POST_INDEXED_INDIRECT;
                opcodeAssembly = "LDA";
                numCycles = 5;
                break;
            case 0xB4:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "LDY";
                numCycles = 4;
                break;
            case 0xB5:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "LDA";
                numCycles = 4;
                break;
            case 0xB6:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_Y;
                opcodeAssembly = "LDX";
                numCycles = 4;
                break;
            case 0xB8:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "CLV";
                numCycles = 2;
                break;
            case 0xB9:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "LDA";
                numCycles = 4;
                break;
            case 0xBA:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "TSX";
                numCycles = 2;
                break;
            case 0xBC:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "LDY";
                numCycles = 4;
                break;
            case 0xBD:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "LDA";
                numCycles = 4;
                break;
            case 0xBE:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "LDX";
                numCycles = 4;
                break;
            case 0xC0:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "CPY";
                numCycles = 2;
                break;
            case 0xC1:
                addressingMode = AddressingMode.PRE_INDEXED_INDIRECT;
                opcodeAssembly = "CMP";
                numCycles = 6;
                break;
            case 0xC4:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "CPY";
                numCycles = 3;
                break;
            case 0xC5:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "CMP";
                numCycles = 3;
                break;
            case 0xC6:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "DEC";
                numCycles = 5;
                break;
            case 0xC8:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "INY";
                numCycles = 2;
                break;
            case 0xC9:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "CMP";
                numCycles = 2;
                break;
            case 0xCA:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "DEX";
                numCycles = 2;
                break;
            case 0xCC:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "CPY";
                numCycles = 4;
                break;
            case 0xCD:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "CMP";
                numCycles = 4;
                break;
            case 0xCE:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "DEC";
                numCycles = 6;
                break;
            case 0xD0:
                addressingMode = AddressingMode.RELATIVE;
                opcodeAssembly = "BNE";
                numCycles = 2;
                break;
            case 0xD1:
                addressingMode = AddressingMode.POST_INDEXED_INDIRECT;
                opcodeAssembly = "CMP";
                numCycles = 5;
                break;
            case 0xD5:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "CMP";
                numCycles = 4;
                break;
            case 0xD6:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "DEC";
                numCycles = 6;
                break;
            case 0xD8:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "CLD";
                numCycles = 2;
                break;
            case 0xD9:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "CMP";
                numCycles = 4;
                break;
            case 0xDD:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "CMP";
                numCycles = 4;
                break;
            case 0xDE:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "DEC";
                numCycles = 7;
                break;
            case 0xE0:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "CPX";
                numCycles = 2;
                break;
            case 0xE1:
                addressingMode = AddressingMode.PRE_INDEXED_INDIRECT;
                opcodeAssembly = "SBC";
                numCycles = 6;
                break;
            case 0xE4:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "CPX";
                numCycles = 3;
                break;
            case 0xE5:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "SBC";
                numCycles = 3;
                break;
            case 0xE6:
                addressingMode = AddressingMode.ZEROPAGE;
                opcodeAssembly = "INC";
                numCycles = 5;
                break;
            case 0xE8:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "INX";
                numCycles = 2;
                break;
            case 0xE9:
                addressingMode = AddressingMode.IMMEDIATE;
                opcodeAssembly = "SBC";
                numCycles = 2;
                break;
            case 0xEA:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "NOP";
                numCycles = 2;
                break;
            case 0xEC:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "CPX";
                numCycles = 4;
                break;
            case 0xED:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "SBC";
                numCycles = 4;
                break;
            case 0xEE:
                addressingMode = AddressingMode.ABSOLUTE;
                opcodeAssembly = "INC";
                numCycles = 6;
                break;
            case 0xF0:
                addressingMode = AddressingMode.RELATIVE;
                opcodeAssembly = "BEQ";
                numCycles = 2;
                break;
            case 0xF1:
                addressingMode = AddressingMode.POST_INDEXED_INDIRECT;
                opcodeAssembly = "SBC";
                numCycles = 5;
                break;
            case 0xF5:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "SBC";
                numCycles = 4;
                break;
            case 0xF6:
                addressingMode = AddressingMode.INDEXED_ZEROPAGE_X;
                opcodeAssembly = "INC";
                numCycles = 6;
                break;
            case 0xF8:
                addressingMode = AddressingMode.IMPLIED;
                opcodeAssembly = "SED";
                numCycles = 2;
                break;
            case 0xF9:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_Y;
                opcodeAssembly = "SBC";
                numCycles = 4;
                break;
            case 0xFD:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "SBC";
                numCycles = 4;
                break;
            case 0xFE:
                addressingMode = AddressingMode.INDEXED_ABSOLUTE_X;
                opcodeAssembly = "INC";
                numCycles = 7;
                break;
            default:
                throw new Exception("This has not been implemented yet.");
        }
    }

    public void executeCommand() {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        boolean updatePC = true;
        switch(opcodeAssembly) {
            case "ADC":
                CPUInstructionList.ADC(cpu, addressedValue, addressingMode);
                break;
            case "AND":
                CPUInstructionList.AND(cpu, addressedValue, addressingMode);
                break;
            case "ASL":
                CPUInstructionList.ASL(cpu, addressedValue, addressingMode);
                break;
            case "BCC":
                CPUInstructionList.BCC(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "BCS":
                CPUInstructionList.BCS(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "BEQ":
                CPUInstructionList.BEQ(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "BIT":
                CPUInstructionList.BIT(cpu, addressedValue, addressingMode);
                break;
            case "BMI":
                CPUInstructionList.BMI(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "BNE":
                CPUInstructionList.BNE(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "BPL":
                CPUInstructionList.BPL(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "BRK":
                CPUInstructionList.BRK(cpu, addressedValue, addressingMode);
                break;
            case "BVC":
                CPUInstructionList.BVC(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "BVS":
                CPUInstructionList.BVS(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "CLC":
                CPUInstructionList.CLC(cpu, addressedValue, addressingMode);
                break;
            case "CLD":
                CPUInstructionList.CLD(cpu, addressedValue, addressingMode);
                break;
            case "CLI":
                CPUInstructionList.CLI(cpu, addressedValue, addressingMode);
                break;
            case "CLV":
                CPUInstructionList.CLV(cpu, addressedValue, addressingMode);
                break;
            case "CMP":
                CPUInstructionList.CMP(cpu, addressedValue, addressingMode);
                break;
            case "CPX":
                CPUInstructionList.CPX(cpu, addressedValue, addressingMode);
                break;
            case "CPY":
                CPUInstructionList.CPY(cpu, addressedValue, addressingMode);
                break;
            case "DEC":
                CPUInstructionList.DEC(cpu, addressedValue, addressingMode);
                break;
            case "DEX":
                CPUInstructionList.DEX(cpu, addressedValue, addressingMode);
                break;
            case "DEY":
                CPUInstructionList.DEY(cpu, addressedValue, addressingMode);
                break;
            case "EOR":
                CPUInstructionList.EOR(cpu, addressedValue, addressingMode);
                break;
            case "INC":
                CPUInstructionList.INC(cpu, addressedValue, addressingMode);
                break;
            case "INX":
                CPUInstructionList.INX(cpu, addressedValue, addressingMode);
                break;
            case "INY":
                CPUInstructionList.INY(cpu, addressedValue, addressingMode);
                break;
            case "JMP":
                CPUInstructionList.JMP(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "JSR":
                CPUInstructionList.JSR(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "LDA":
                CPUInstructionList.LDA(cpu, addressedValue, addressingMode);
                break;
            case "LDX":
                CPUInstructionList.LDX(cpu, addressedValue, addressingMode);
                break;
            case "LDY":
                CPUInstructionList.LDY(cpu, addressedValue, addressingMode);
                break;
            case "LSR":
                CPUInstructionList.LSR(cpu, addressedValue, addressingMode);
                break;
            case "NOP":
                CPUInstructionList.NOP(cpu, addressedValue, addressingMode);
                break;
            case "ORA":
                CPUInstructionList.ORA(cpu, addressedValue, addressingMode);
                break;
            case "PHA":
                CPUInstructionList.PHA(cpu, addressedValue, addressingMode);
                break;
            case "PHP":
                CPUInstructionList.PHP(cpu, addressedValue, addressingMode);
                break;
            case "PLA":
                CPUInstructionList.PLA(cpu, addressedValue, addressingMode);
                break;
            case "PLP":
                CPUInstructionList.PLP(cpu, addressedValue, addressingMode);
                break;
            case "ROL":
                CPUInstructionList.ROL(cpu, addressedValue, addressingMode);
                break;
            case "ROR":
                CPUInstructionList.ROR(cpu, addressedValue, addressingMode);
                break;
            case "RTI":
                CPUInstructionList.RTI(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "RTS":
                CPUInstructionList.RTS(cpu, addressedValue, addressingMode);
                updatePC = false;
                break;
            case "SBC":
                CPUInstructionList.SBC(cpu, addressedValue, addressingMode);
                break;
            case "SEC":
                CPUInstructionList.SEC(cpu, addressedValue, addressingMode);
                break;
            case "SED":
                CPUInstructionList.SED(cpu, addressedValue, addressingMode);
                break;
            case "SEI":
                CPUInstructionList.SEI(cpu, addressedValue, addressingMode);
                break;
            case "STA":
                CPUInstructionList.STA(cpu, addressedValue, addressingMode);
                break;
            case "STX":
                CPUInstructionList.STX(cpu, addressedValue, addressingMode);
                break;
            case "STY":
                CPUInstructionList.STY(cpu, addressedValue, addressingMode);
                break;
            case "TAX":
                CPUInstructionList.TAX(cpu, addressedValue, addressingMode);
                break;
            case "TAY":
                CPUInstructionList.TAY(cpu, addressedValue, addressingMode);
                break;
            case "TSX":
                CPUInstructionList.TSX(cpu, addressedValue, addressingMode);
                break;
            case "TXA":
                CPUInstructionList.TXA(cpu, addressedValue, addressingMode);
                break;
            case "TXS":
                CPUInstructionList.TXS(cpu, addressedValue, addressingMode);
                break;
            case "TYA":
                CPUInstructionList.TYA(cpu, addressedValue, addressingMode);
                break;
        }
        if (updatePC) {
            cpuRegisterState.PC += addressingMode.getNumBytes();
            cpuRegisterState.PC = cpuRegisterState.PC & 0xFFFF;
        }
    }


    @Override
    public String toString() {
        return opcodeAssembly + " " + addressingMode;
    }
}
