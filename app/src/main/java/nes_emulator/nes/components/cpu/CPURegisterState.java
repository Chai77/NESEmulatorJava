package nes_emulator.nes.components.cpu;

public class CPURegisterState {
    public int A; // accumulator register - byte
    public int X; // index register 1 - byte
    public int Y; // index register 2 - byte
    public int PC; // program counter - short
    public int S; // stack pointer - byte
    public int P; // status register - byte

    // constructor that creates the RegisterState with certain values
    public CPURegisterState(int A, int X, int Y, int PC, int S, int P) {
        this.A = A;
        this.X = X;
        this.Y = Y;
        this.PC = PC;
        this.S = S;
        this.P = P;
    }

    public CPURegisterState() {
        A = 0;
        X = 0;
        Y = 0;
        PC = 0x4020; // TODO: make the mapper set this to a value
        S = 0xFF;
        P = 0;
    }

    // status register methods

    // get bits
    private int getBitAt(int num, int pos) {
        return (num >> pos) & 1;
    }

    public int getStatusN() {
        return getBitAt(P, 7);
    }

    public int getStatusV() {
        return getBitAt(P, 6);
    }

    public int getStatusB() {
        return getBitAt(P, 4);
    }

    public int getStatusD() {
        return getBitAt(P, 3);
    }

    public int getStatusI() {
        return getBitAt(P, 2);
    }

    public int getStatusZ() {
        return getBitAt(P, 1);
    }

    public int getStatusC() {
        return getBitAt(P, 0);
    }

    public int getStatusToPush() {
        int returnP = changeBitAt(P, 1, 4);
        returnP = changeBitAt(returnP, 1, 5);
        return returnP;
    }

    public void readStatusFromStack(int stackP) {
        stackP = changeBitAt(stackP, 0, 4);
        stackP = changeBitAt(stackP, 1, 5);
        P = stackP;
    }




    //set bits
    private int changeBitAt(int original, int newBit, int pos) {
        int changed = -1;
        if (newBit == 1) {
            changed = (original | (1 << pos)) & 0xFF;
        } else {
            changed = (original & ~(1 << pos)) & 0xFF;
        }
        return changed;
    }

    private void setStatusAtBit(int value, int pos) {
        P = changeBitAt(P, value, pos);
    }

    public void setStatusN(int value) {
        setStatusAtBit(value, 7);
    }

    public void setStatusV(int value) {
        setStatusAtBit(value, 6);
    }

    public void setStatusB(int value) {
        setStatusAtBit(value, 4);
    }

    public void setStatusD(int value) {
        setStatusAtBit(value, 3);
    }

    public void setStatusI(int value) {
        setStatusAtBit(value, 2);
    }

    public void setStatusZ(int value) {
        setStatusAtBit(value, 1);
    }

    public void setStatusC(int value) {
        setStatusAtBit(value, 0);
    }
}
