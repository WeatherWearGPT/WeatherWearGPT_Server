package com.weatherweargpt;

import java.io.FileWriter;
import java.io.IOException;

public class FileWriteTest {

    public static void main(String[] args) {
        String directoryPath = "/home/ubuntu/docker-volume/img/";  // 테스트할 경로
        String testFileName = "test_write.txt";
        
        try (FileWriter writer = new FileWriter(directoryPath + testFileName)) {
            writer.write("Test write success!");
            System.out.println("Successfully wrote to the file: " + directoryPath + testFileName);
        } catch (IOException e) {
            System.err.println("Failed to write to the file: " + e.getMessage());
        }
    }
}
