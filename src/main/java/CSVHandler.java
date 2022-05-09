import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

class CSVHandler {
    private CSVHandler(){}

    static ArrayList<ArrayList<String>> grabAllFiles(String directory, ArrayList<String> fileNames, boolean debugMode){
        ArrayList<ArrayList<String>> allFileData = readCSV(directory + fileNames.remove(0));
        for(String fileName : fileNames){
            if(debugMode) System.out.println("Loading File " + fileName + "...");
            ArrayList<ArrayList<String>> fileContent = readCSV(directory + fileName);
            fileContent.remove(0);
            allFileData.addAll(fileContent);
        }
        return allFileData;
    }

    static void writeCSV(String directory, ArrayList<String[]> toWrite, boolean debugMode){
        CSVWriter csvWriter = null;
        try {
            if(debugMode) System.out.println("Writing file at " + directory + "...");
            csvWriter = new CSVWriter(new FileWriter(directory));
            csvWriter.writeAll(toWrite);

        }catch (java.io.IOException e){
            System.out.println("An error occurred when attempting to write a file.");
            System.out.println("Message: \n" + e.getMessage());
            System.out.println("StackTrace:");
            e.printStackTrace();
        }finally {
            try {
                if(csvWriter!=null)
                    csvWriter.close();
            } catch (java.io.IOException e) {
                System.out.println("An error occurred when attempting to close a file connection.");
                System.out.println("Message: \n" + e.getMessage());
                System.out.println("StackTrace:");
            }
        }
    }

    private static ArrayList<ArrayList<String>> readCSV(String directory){
        ArrayList<ArrayList<String>> raw = new ArrayList<>();

        System.out.println("Reading raw data from " + directory + "... ");
        try {
            CSVReader reader = new CSVReader(new FileReader(directory));
            String[] curr;
            while ((curr = reader.readNext()) != null) {
                raw.add(Arrays.stream(curr).collect(Collectors.toCollection(ArrayList::new)));
            }
        }
        catch (FileNotFoundException e){
            System.err.println("Error: CSV File not found");
            e.printStackTrace();
            System.exit(1);
        }
        catch (IOException e){
            System.err.println("Error: OpenCSV IOException Caught");
            e.printStackTrace();
            System.exit(1);
        }

        return raw;
    }
}
