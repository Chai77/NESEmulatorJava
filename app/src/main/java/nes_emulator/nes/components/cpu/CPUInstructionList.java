package nes_emulator.nes.components.cpu;

import nes_emulator.nes.components.bus.MainBus;

public class CPUInstructionList {

    // on branch or jump or return, don't update PC

    private static int add(CPURegisterState cpuRegisterState, int first, int second, int c, boolean updateV) {
        int result = first + second + c;

        int carry_bit = (result >> 8) & 1;
        int overflow_bit = 0;

        boolean f7 = ((first >> 7) & 1) == 1;
        boolean s7 = ((second >> 7) & 1) == 1;
        boolean o7 = ((result >> 7) & 1) == 1;

        if ((!f7 && !s7 && o7) || (f7 && s7 && !o7)) {
            overflow_bit = 1;
        }

        cpuRegisterState.setStatusC(carry_bit);
        if (updateV) {
            cpuRegisterState.setStatusV(overflow_bit);
        }

        return result & 0xFF;
    }

    private static int subtract(CPURegisterState cpuRegisterState, int first, int second, int c, boolean updateV) {
        return add(cpuRegisterState, first, (~second) & 0xFF, 1 - c, updateV);
    }

    private static void updateStatusFlags(CPURegisterState cpuRegisterState, int result) {
        int zeroFlag = 0;
        if (result == 0) {
            zeroFlag = 1;
        }

        int negativeFlag = 0;
        if (((result >> 7) & 1) == 1) {
            negativeFlag = 1;
        }

        cpuRegisterState.setStatusN(negativeFlag);
        cpuRegisterState.setStatusZ(zeroFlag);
    }

    public static void ADC(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = add(cpuRegisterState, addressingResult.value, cpuRegisterState.A, cpuRegisterState.getStatusC(), true);
        cpuRegisterState.A = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void AND(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.A = cpuRegisterState.A & addressingResult.value;

        updateStatusFlags(cpuRegisterState, cpuRegisterState.A);
    }

    public static void ASL(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {

        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        int result = (addressingResult.value << 1) & 0xFF;
        if (addressingMode == CPUInstruction.AddressingMode.ACCUMULATOR) {
            cpuRegisterState.A = result;
        } else {
            mainBus.writeByte(addressingResult.address, result);
        }
        updateStatusFlags(cpuRegisterState, result);
        cpuRegisterState.setStatusC((addressingResult.value >> 7) & 1);
    }

    public static void BCC(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        // update num cycles for cpu
        if (cpuRegisterState.getStatusC() == 0) {
            cpuRegisterState.PC = addressingResult.address;
        } else {
            cpuRegisterState.PC += 2;
            cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
        }
    }

    public static void BCS(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        // update num cycles for cpu
        if (cpuRegisterState.getStatusC() == 1) {
            cpuRegisterState.PC = addressingResult.address;
        } else {
            cpuRegisterState.PC += 2;
            cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
        }
    }

    public static void BEQ(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        // update num cycles for cpu
        if (cpuRegisterState.getStatusZ() == 1) {
            cpuRegisterState.PC = addressingResult.address;
        } else {
            cpuRegisterState.PC += 2;
            cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
        }
    }

    public static void BIT(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        cpuRegisterState.setStatusV((addressingResult.value >> 6) & 1);
        cpuRegisterState.setStatusN((addressingResult.value >> 7) & 1);

        int zero_bit = 0;
        if ((cpuRegisterState.A & addressingResult.value) == 0) {
            zero_bit = 1;
        }
        cpuRegisterState.setStatusZ(zero_bit);
    }

    public static void BMI(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        // update num cycles for cpu
        if (cpuRegisterState.getStatusN() == 1) {
            cpuRegisterState.PC = addressingResult.address;
        } else {
            cpuRegisterState.PC += 2;
            cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
        }
    }

    public static void BNE(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        // update num cycles for cpu
        if (cpuRegisterState.getStatusZ() == 0) {
            cpuRegisterState.PC = addressingResult.address;
        } else {
            cpuRegisterState.PC += 2;
            cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
        }
    }

    public static void BPL(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        // update num cycles for cpu
        if (cpuRegisterState.getStatusN() == 0) {
            cpuRegisterState.PC = addressingResult.address;
        } else {
            cpuRegisterState.PC += 2;
            cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
        }
    }

    public static void BRK(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        // TODO: implement break, which creates an interrupt with the return being the next instruction
        // Need to implement interrupt logic first
        // interrupt disable flag might be set
        // something with the B flag too

        CPURegisterState cpuRegisterState = cpu.getRegisters();
        CPUStack cpuStack = cpu.getCpuStack();
    

        int PC_pushed = cpuRegisterState.PC + 2;
        cpuStack.push((PC_pushed >> 8) & 0xFF);
        cpuStack.push(PC_pushed & 0xFF);

        cpuStack.push(cpuRegisterState.getStatusToPush());

        cpuRegisterState.setStatusI(1);

        // TODO: make interrupt happen
    }

    public static void BVC(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        // update num cycles for cpu
        if (cpuRegisterState.getStatusV() == 0) {
            cpuRegisterState.PC = addressingResult.address;
        } else {
            cpuRegisterState.PC += 2;
            cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
        }
    }

    public static void BVS(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        // update num cycles for cpu
        if (cpuRegisterState.getStatusV() == 1) {
            cpuRegisterState.PC = addressingResult.address;
        } else {
            cpuRegisterState.PC += 2;
            cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
        }
    }


    // clear
    public static void CLC(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.setStatusC(0);
    }

    public static void CLD(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.setStatusD(0);
    }

    public static void CLI(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.setStatusI(0);
    }

    public static void CLV(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.setStatusV(0);
    }

    // compare
    public static void CMP(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = subtract(cpuRegisterState, cpuRegisterState.A, addressingResult.value, 1, false);

        updateStatusFlags(cpuRegisterState, result);
    }

    // compare
    public static void CPX(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = subtract(cpuRegisterState, cpuRegisterState.X, addressingResult.value, 1, false);

        updateStatusFlags(cpuRegisterState, result);
    }

    // compare
    public static void CPY(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = subtract(cpuRegisterState, cpuRegisterState.Y, addressingResult.value, 1, false);

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void DEC(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        int result = addressingResult.value - 1;
        mainBus.writeByte(addressingResult.address, result);

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void DEX(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.X - 1;
        cpuRegisterState.X = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void DEY(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.Y - 1;
        cpuRegisterState.Y = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void EOR(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.A ^ addressingResult.value;
        cpuRegisterState.A = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void INC(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        int result = addressingResult.value + 1;
        mainBus.writeByte(addressingResult.address, result);

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void INX(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.X + 1;
        cpuRegisterState.X = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void INY(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.Y + 1;
        cpuRegisterState.Y = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void JMP(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.PC = addressingResult.address;
    }

    public static void JSR(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        CPUStack cpuStack = cpu.getCpuStack();
        int PC_pushed = cpuRegisterState.PC + 2;
        cpuStack.push((PC_pushed >> 8) & 0xFF);
        cpuStack.push(PC_pushed & 0xFF);

        cpuRegisterState.PC = addressingResult.address;
    }

    public static void LDA(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = addressingResult.value;
        cpuRegisterState.A = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void LDX(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = addressingResult.value;
        cpuRegisterState.X = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void LDY(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = addressingResult.value;
        cpuRegisterState.Y = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void LSR(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        int result = (addressingResult.value >> 1) & 0xFF;
        if (addressingMode == CPUInstruction.AddressingMode.ACCUMULATOR) {
            cpuRegisterState.A = result;
        } else {
            mainBus.writeByte(addressingResult.address, result);
        }

        updateStatusFlags(cpuRegisterState, result);
        cpuRegisterState.setStatusC(addressingResult.value & 1);
    }

    public static void NOP(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
    }

    public static void ORA(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.A = cpuRegisterState.A | addressingResult.value;

        updateStatusFlags(cpuRegisterState, cpuRegisterState.A);
    }

    public static void PHA(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        CPUStack cpuStack = cpu.getCpuStack();
        cpuStack.push(cpuRegisterState.A);
    }

    public static void PHP(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        CPUStack cpuStack = cpu.getCpuStack();
        cpuStack.push(cpuRegisterState.getStatusToPush());
    }

    public static void PLA(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        CPUStack cpuStack = cpu.getCpuStack();
        int result = cpuStack.pop();

        cpuRegisterState.A = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void PLP(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        CPUStack cpuStack = cpu.getCpuStack();
        int result = cpuStack.pop();

        cpuRegisterState.readStatusFromStack(result);
    }

    public static void ROL(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        int result = ((addressingResult.value << 1) & 0xFF) | (cpuRegisterState.getStatusC() & 1);
        if (addressingMode == CPUInstruction.AddressingMode.ACCUMULATOR) {
            cpuRegisterState.A = result;
        } else {
            mainBus.writeByte(addressingResult.address, result);
        }

        updateStatusFlags(cpuRegisterState, result);
        cpuRegisterState.setStatusC((addressingResult.value >> 7) & 1);
    }

    public static void ROR(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        int result = ((addressingResult.value >> 1) & 0xFF) | ((cpuRegisterState.getStatusC() << 7) & 0xFF);
        if (addressingMode == CPUInstruction.AddressingMode.ACCUMULATOR) {
            cpuRegisterState.A = result;
        } else {
            mainBus.writeByte(addressingResult.address, result);
        }

        updateStatusFlags(cpuRegisterState, result);
        cpuRegisterState.setStatusC(addressingResult.value & 1);
    }

    public static void RTI(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        CPUStack cpuStack = cpu.getCpuStack();
        cpuRegisterState.readStatusFromStack(cpuStack.pop());

        int PCL = cpuStack.pop();
        int PCH = cpuStack.pop();
        int newPC = ((PCH << 8) | PCL) & 0xFFFF;
        cpuRegisterState.PC = newPC;
    }

    public static void RTS(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        CPUStack cpuStack = cpu.getCpuStack();
        int PCL = cpuStack.pop();
        int PCH = cpuStack.pop();

        int newPC = ((PCH << 8) | PCL) & 0xFFFF;
        cpuRegisterState.PC = newPC + 1;
        cpuRegisterState.PC = cpuRegisterState.PC % 0xFFFF;
    }


    public static void SBC(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = subtract(cpuRegisterState, cpuRegisterState.A, addressingResult.value, (~cpuRegisterState.getStatusC()) & 1, true);
        cpuRegisterState.A = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    // set
    public static void SEC(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.setStatusC(1);
    }

    public static void SED(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.setStatusD(1);
    }

    public static void SEI(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        cpuRegisterState.setStatusI(1);
    }

    public static void STA(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        mainBus.writeByte(addressingResult.address, cpuRegisterState.A);
    }

    public static void STX(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        mainBus.writeByte(addressingResult.address, cpuRegisterState.X);
    }

    public static void STY(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        mainBus.writeByte(addressingResult.address, cpuRegisterState.Y);
    }

    public static void TAX(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        MainBus mainBus = cpu.getMainBus();

        int result = cpuRegisterState.A;
        cpuRegisterState.X = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void TAY(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.A;
        cpuRegisterState.Y = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void TSX(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.S;
        cpuRegisterState.Y = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void TXA(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.X;
        cpuRegisterState.A = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void TXS(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.X;
        cpuRegisterState.S = result;

        updateStatusFlags(cpuRegisterState, result);
    }

    public static void TYA(CPU cpu, ValueWithMemory addressingResult, CPUInstruction.AddressingMode addressingMode) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();

        int result = cpuRegisterState.Y;
        cpuRegisterState.A = result;

        updateStatusFlags(cpuRegisterState, result);
    }
}
