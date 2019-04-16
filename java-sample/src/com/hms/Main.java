package com.hms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        String component = "hydrology";
        String dataset = "precipitation";
        String source = "nldas";
        String startdate = "2010-01-01";
        String enddate = "2010-12-31";
        Map<String, String> geometry = new HashMap<String, String>();
        geometry.put("latitude", "33.325");
        geometry.put("longitude", "-83.525");
        String timestep = "daily";

        JavaSample hms = new JavaSample(component, dataset, source, startdate, enddate, geometry, timestep);
        try {
            hms.submitRequest();
            hms.getData();
            hms.writeToFile();
        }
        catch(IOException e){
            System.out.println("Error: IO Exception encountered.");
        }
    }
}
