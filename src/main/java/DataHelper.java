import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

class DataHelper {
    static void processData(String processedDataDirectory, boolean debugMode){
        if(debugMode) System.out.println("Loading Raw Data... ");

        String dataDirectory = "Data/Raw/";
        ArrayList<ArrayList<String>> vaersData = CSVHandler.grabAllFiles(dataDirectory,
                new ArrayList<>(Arrays.asList("2020VAERSDATA.csv", "2021VAERSDATA.csv", "2022VAERSDATA.csv")),
                debugMode);
        ArrayList<ArrayList<String>> vaersVax = CSVHandler.grabAllFiles(dataDirectory,
                new ArrayList<>(Arrays.asList("2020VAERSVAX.csv", "2021VAERSVAX.csv", "2022VAERSVAX.csv")),
                debugMode);

        if(debugMode) System.out.println("Raw Data Successfully Loaded... ");

        HashMap<String, Integer> categoryMap = new HashMap<>();
        int numCategories = 7;
        //Categories: Infant, Child, Adult, Male, Female, 2020, 2021
        //Elderly, Unknown Gender, and 2022 are all implied from all 0 values

        ArrayList<ArrayList<String>> data = new ArrayList<>();
        ArrayList<String> ivData = new ArrayList<>();


        if(debugMode) System.out.println("Finding valid entries... ");

        for(int i = 1; i < vaersVax.size(); i++){// Looping through all Vaccine data
            if(debugMode) System.out.println( i + "/" + vaersVax.size() + "    " + "Current Num Features: " + numCategories);

            if(!vaersVax.get(i).contains("")) { // If there's no missing data entries

                for (ArrayList<String> vaersDatum : vaersData) { // Loop through Vaers data
                    // If id is the same, create a data entry
                    if (vaersDatum.get(0).equals(vaersVax.get(i).get(0)) && (!vaersDatum.contains(""))) {
                        //initialize entry with default category count
                        ArrayList<String> entry = new ArrayList<>(Arrays.asList("0.0", "0.0", "0.0", "0.0", "0.0", "0.0", "0.0"));

                        // <--------------- Age variable --------------->
                        double age = Double.parseDouble(vaersDatum.get(1));
                        if (age < 1) { //if Infant
                            entry.set(0, "1.0");
                        } else if (age < 18) { //if Child
                            entry.set(1, "1.0");
                        } else if (age < 60) { //if Adult
                            entry.set(2, "1.0");
                        } //else, Elderly

                        // <--------------- Gender variable --------------->
                        switch(vaersDatum.get(2)){
                            case "M":
                                entry.set(3, "1.0");
                                break;
                            case "F":
                                entry.set(4, "1.0");
                        }// else, unknown gender

                        // <--------------- Date variable --------------->
                        String date = vaersDatum.get(4);
                        int yearIndex = date.indexOf("/", date.indexOf("/") + 1) + 1;
                        switch(date.substring(yearIndex)){
                            case "2020":
                                entry.set(5, "1.0");
                                break;
                            case "2021":
                                entry.set(6, "1.0");
                        }// else, 2022

                        // <--------------- Vaccine variable --------------->
                        String vax = vaersVax.get(i).get(2);
                        for (int k = 0; k < numCategories-7; k++)
                            entry.add("0.0");

                        if (categoryMap.containsKey(vax))
                            entry.set(categoryMap.get(vax), "1.0");
                        else {
                            addNewCategory(vax, numCategories, categoryMap, data, entry);
                            numCategories++;
                        }

                        // <--------------- Manufacturer variable --------------->
                        String manu = vaersVax.get(i).get(1);
                        if (categoryMap.containsKey(manu))
                            entry.add(categoryMap.get(manu), "1.0");
                        else {
                            addNewCategory(manu, numCategories, categoryMap, data, entry);
                            numCategories++;
                        }
                        data.add(entry);

                        // <--------------- independent variable (Died) --------------->
                        if (vaersDatum.get(3).equals("Y"))
                            ivData.add("1.0");
                        else
                            ivData.add("0.0");
                    }
                }
            }
        }
        // Add independent variable data as last element in all data entries
        for (int i = 0; i<ivData.size(); i++)
            data.get(i).add(ivData.get(i));

        writeProcessedData(data, processedDataDirectory, debugMode);
    }

    private static void addNewCategory(String key, Integer value, HashMap<String, Integer> categoryMap,
                                ArrayList<ArrayList<String>> data, ArrayList<String> currentEntry){
        currentEntry.add("1.0");
        categoryMap.put(key, value);
        for (ArrayList<String> curr : data)
            curr.add("0.0");
    }

    private static void writeProcessedData(ArrayList<ArrayList<String>> data, String directory, boolean debugMode){
        ArrayList<String[]> toWrite = new ArrayList<>();
        for (ArrayList<String> entry : data)
            toWrite.add(entry.toArray(new String[0]));
        CSVHandler.writeCSV(directory, toWrite, debugMode);
    }

    private DataHelper(){}
}
