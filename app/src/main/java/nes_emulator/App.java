/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package nes_emulator;

import nes_emulator.nes.components.cpu.CPU;
import nes_emulator.nes.components.cpu.CPURegisterState;
import nes_emulator.nes.components.memory.CPUMemory;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class App {
    public static void main(String[] args) throws FileNotFoundException, IOException, org.json.simple.parser.ParseException {
        CPU cpu = new CPU();
        
        
        String[] hexArr = new String[0x100];
        for (int i = 0; i <= 0xFF; i++) {
            String hex = Integer.toHexString(i);
            if (hex.length() < 2) {
                hex = "0" + hex; // Add leading zero if necessary
            }
            hexArr[i] = hex;
        }

        int[][] results = new int[hexArr.length][2];

        // for (int i = 0; i < 1; i++) {
        for (int i = 0; i < hexArr.length; i++) {
            System.out.println("Running tests for opcode " + hexArr[i]);
            int[] testsPassed = runTestForOpcode(cpu, hexArr[i]);
            System.out.println(testsPassed[0] + "/" + testsPassed[1] + " for opcode " + hexArr[i]);
            results[i][0] = testsPassed[0];
            results[i][1] = testsPassed[1];
        }

        System.out.println("==============================================");
        System.out.println("==============================================");
        System.out.println("==============================================");
        System.out.println("==============================================");
        System.out.println("Final results: ");
        for (int i = 0; i < results.length; i++) {
            if (results[i][0] == -1) continue;
            if (results[i][0] == results[i][1]) {
                System.out.println("Opcode " + hexArr[i] + ": Passed");
            } else {
                System.out.println("Opcode: " + hexArr[i] + ": Failed " + results[i][0] + "/" + results[i][1]);

            }
        }
    }

    public static int[] runTestForOpcode(CPU cpu, String opcode)  throws IOException, ParseException {
        String filename = "/home/chaitanyae/Documents/Projects/NESEmulatorJava/test/" + opcode + ".json";
        JSONParser parser = new JSONParser();
        Reader reader = new FileReader(filename);

        JSONArray opcodeList = (JSONArray) parser.parse(reader);

        int num_correct = 0;
        for (Object obj : opcodeList) {
            try {
                num_correct += (runTest(cpu, (JSONObject) obj) ? 1 : 0);
            } catch (Exception e) {
                return new int[]{-1, opcodeList.size()};
            }
        }
        return new int[]{num_correct, opcodeList.size()};
    }

    public static boolean runTest(CPU cpu, JSONObject main) throws Exception {

        JSONObject initial_vals = (JSONObject) main.get("initial");
        JSONObject final_vals = (JSONObject) main.get("final");

        initializeWithInitialVals(cpu, initial_vals);

        cpu.executeNextCmd();

        String diff = checkCorrectFinalResult(cpu, final_vals);

        if (!diff.equals("")) {
            System.out.println("=================================");
            System.out.println((String) main.get("name"));
            System.out.println(initial_vals);
            System.out.println(diff);
            System.out.println("=================================");
        }

        return diff.equals("");
    }

    public static void initializeWithInitialVals(CPU cpu, JSONObject initial_vals) {
        Long pc = (Long) initial_vals.get("pc");
        Long s = (Long) initial_vals.get("s");
        Long a = (Long) initial_vals.get("a");
        Long y = (Long) initial_vals.get("y");
        Long x = (Long) initial_vals.get("x");
        Long p = (Long) initial_vals.get("p");

        JSONArray ram = (JSONArray) initial_vals.get("ram");
        int[][] ram_out = new int[ram.size()][2];
        int i = 0;
        for (Object one_ram : ram) {
            JSONArray one_ram_arr = (JSONArray) one_ram;
            Long address = (Long) one_ram_arr.get(0);
            Long value = (Long) one_ram_arr.get(1);

            ram_out[i][0] = address.intValue();
            ram_out[i][1] = value.intValue();

            i++;
        }

        CPURegisterState cpuRegisterState = cpu.getRegisters();
        CPUMemory cpuMemory = cpu.getCpuMemory();

        // initializing registers
        cpuRegisterState.A = a.intValue();
        cpuRegisterState.PC = pc.intValue();
        cpuRegisterState.S = s.intValue();
        cpuRegisterState.X = x.intValue();
        cpuRegisterState.Y = y.intValue();
        cpuRegisterState.P = p.intValue();

        // initializing ram
        cpuMemory.initialize(ram_out);
    }

    public static String checkCorrectFinalResult(CPU cpu, JSONObject final_vals) {
        CPURegisterState cpuRegisterState = cpu.getRegisters();
        CPUMemory cpuMemory = cpu.getCpuMemory();

        Long pc = (Long) final_vals.get("pc");
        Long s = (Long) final_vals.get("s");
        Long a = (Long) final_vals.get("a");
        Long y = (Long) final_vals.get("y");
        Long x = (Long) final_vals.get("x");
        Long p = (Long) final_vals.get("p");

        JSONArray ram = (JSONArray) final_vals.get("ram");
        int[][] ram_out = new int[ram.size()][2];
        int i = 0;
        for (Object one_ram : ram) {
            JSONArray one_ram_arr = (JSONArray) one_ram;
            Long address = (Long) one_ram_arr.get(0);
            Long value = (Long) one_ram_arr.get(1);

            ram_out[i][0] = address.intValue();
            ram_out[i][1] = value.intValue();

            i++;
        }

        ArrayList<int[]> resultRam = cpuMemory.getMemoryArr();

        String diffString = "";

        if (cpuRegisterState.PC != pc.intValue()) {
            diffString += "PC is not the same: got " + cpuRegisterState.PC + " expected " + pc.intValue() + "\n";
        }
        if (cpuRegisterState.S != s.intValue()) {
            diffString += "S is not the same: got " + cpuRegisterState.S + " expected " + s.intValue() + "\n";
        }
        if (cpuRegisterState.A != a.intValue()) {
            diffString += "A is not the same: got " + cpuRegisterState.A + " expected " + a.intValue() + "\n";
        }
        if (cpuRegisterState.Y != y.intValue()) {
            diffString += "Y is not the same: got " + cpuRegisterState.Y + " expected " + y.intValue() + "\n";
        }
        if (cpuRegisterState.X != x.intValue()) {
            diffString += "X is not the same: got " + cpuRegisterState.X + " expected " + x.intValue() + "\n";
        }
        if (cpuRegisterState.P != p.intValue()) {
            diffString += "P is not the same: got " + cpuRegisterState.P + " expected " + p.intValue() + "\n";
        }

        int numIntersect = 0;

        boolean ram_diff = false;

        for (i = 0; i < resultRam.size(); i++) {
            for (int k = 0; k < ram_out.length; k++) {
                if (resultRam.get(i)[0] == ram_out[k][0]) {
                    if (resultRam.get(i)[1] != ram_out[k][1]) {
                        ram_diff = true;
                    }
                    numIntersect++;
                    break;
                }
            }
        }

        if (numIntersect != resultRam.size() || numIntersect != ram_out.length) {
            ram_diff = true;
        }

        if (ram_diff) {
            diffString += "There is a difference in the ram: \n";
            diffString += "expected: \n";
            for (i = 0; i < ram_out.length; i++) {
                diffString += "    [" + ram_out[i][0] + ", " + ram_out[i][1] + "]\n";
            }
            diffString += "\ngotten: \n";
            for (i = 0; i < resultRam.size(); i++) {
                diffString += "    [" + resultRam.get(i)[0] + ", " + resultRam.get(i)[1] + "]\n";
            }
        }


        return diffString;
    }
}
