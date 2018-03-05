package ru.ifmo.rain.glukhov.walk;

import java.io.*;

import static java.lang.Math.min;

public class Walk {
    static private int BLOCK_LENGTH = 1024;
    static private int x0 = 0x01000193;

    public static void main(String[] args) {
        if (args == null) {
            System.err.println("Error: null args");
            return;
        } else if (args.length < 2) {
            System.err.println("Error: incorrect amount of arguments");
            return;
        } else if (args[0] == null || args[1] == null) {
                System.err.println("Error: i/o file null args");
                return;
            }
        try (BufferedReader fileReader = new BufferedReader(new FileReader(args[0]));
             BufferedWriter fileWriter = new BufferedWriter(new FileWriter(args[1]))) {
            String path = "";
            while ((path = fileReader.readLine()) != null) {
                int hval = 0x811c9dc5;
                try (InputStream reader = new FileInputStream(path)) {
                    while (reader.available() > 0) {
                        int size = min(BLOCK_LENGTH, reader.available());
                        byte[] block = new byte[size];
                        int tmp = reader.read(block, 0, size);
                        for (int i = 0; i < size; i++) {
                            hval = (hval * x0) ^ (block[i] & 255);
                        }
                    }
                    fileWriter.write(String.format("%08x", hval) + " " + path + '\n');
                    reader.close();
                } catch (FileNotFoundException e) {
                    fileWriter.write(String.format("%08x", 0) + " " + path + '\n');
                }
            }
            fileReader.close();
            fileWriter.close();
        } catch (FileNotFoundException e) {
            System.err.println("Error: input/output file not found");
        } catch (IOException e) {
            System.err.println("Error: input/output");
        }
    }
}