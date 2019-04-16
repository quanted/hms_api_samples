package com.hms;

import java.io.*;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

/**
 * Java sample code for retrieving data using the HMS API
 */
public class JavaSample {

    private final String USER_AGENT = "Mozilla/5.0";

    private final String requestURL = "https://qed.epacdx.net/hms/rest/api/v3/";
    private final String dataURL = "https://qed.epacdx.net/hms/rest/api/v2/hms/data?job_id=";

    // Request parameters
    private Request request;
    private String component = null;
    private String dataset = null;
    private JSONObject task = null;
    private boolean completed = false;
    private JSONObject taskResponse = null;


    /**
     * Class Constructor
     * @param component HMS Component
     * @param dataset HMS Dataset
     * @param source Timeseries data source
     * @param startDate Timeseries start date
     * @param endDate Timeseries end date
     * @param geometry Timeseries geospatial area of interest
     * @param timestep Timeseries temporal resolution
     */
    public JavaSample(String component, String dataset, String source, String startDate, String endDate, Map<String, String> geometry, String timestep){
        System.out.println("Initializing request object.");
        this.component = component;
        this.dataset = dataset;
        if(geometry.containsKey("latitude")){
            this.request = new Request(source, startDate, endDate, Double.parseDouble(geometry.get("latitude")), Double.parseDouble(geometry.get("longitude")), timestep);
        }
        else if(geometry.containsKey("comID")){
            this.request = new Request(source, startDate, endDate, Integer.parseInt(geometry.get("comID")), timestep);
        }
        else if(geometry.containsKey("stationID")){
            this.request = new Request(source, startDate, endDate, geometry.get("stationID"), timestep);
        }
    }

    /**
     * Submit POST request for data from HMS
     * @throws IOException
     */
    public void submitRequest() throws IOException {
        System.out.println("Submitting request for data to HMS API.");
        String url = this.requestURL + this.component + "/" + this.dataset + "/";
        URL request = new URL(url);
        HttpURLConnection con = (HttpURLConnection)request.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", this.USER_AGENT);
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setDoOutput(true);
        OutputStream dos = con.getOutputStream();
        String requestString = this.request.toString();
        dos.write(requestString.getBytes());
        dos.flush();
        dos.close();

        int responseCode = con.getResponseCode();
        StringBuffer response = new StringBuffer();

        if(responseCode == HttpURLConnection.HTTP_OK){
            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            while((inputLine = reader.readLine()) != null){
                response.append(inputLine);
            }
            reader.close();
        }
        this.task = new JSONObject(response.toString());
        System.out.println("Data submission completed.");
    }

    /**
     * Query HMS Data API for status of data retrieval task.
     * @throws IOException
     */
    public void getData() throws IOException{
        System.out.println("Checking HMS Data API for task status.");
        this.completed = false;
        int delay = 5000;
        while(!completed){
            try {
                Thread.sleep(delay);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
            String url = this.dataURL + this.task.get("job_id");
            URL request = new URL(url);
            HttpURLConnection con = (HttpURLConnection)request.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", this.USER_AGENT);
            int responseCode = con.getResponseCode();
            StringBuffer response = new StringBuffer();
            if(responseCode == HttpURLConnection.HTTP_OK){
                BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                while((inputLine = reader.readLine()) != null){
                    response.append(inputLine);
                }
                this.taskResponse = new JSONObject(response.toString());
                reader.close();
            }
            else{
                System.out.println("Error: Failed to connect to server.");
                this.completed = true;
            }
            if(this.taskResponse != null){
                if(this.taskResponse.get("status").equals("SUCCESS")) {
                    this.completed = true;
                    System.out.println("Task completed successfully, data download complete.");
                }
                else if(this.taskResponse.get("status") == "FAILURE") {
                    this.completed = true;
                    System.out.println("Error: Task failed to complete.");
                }
            }
        }
    }

    /**
     * Write timeseries data to json file
     * @throws IOException
     */
    public void writeToFile() throws IOException{
        String fileName = "hms-data.json";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        if(this.taskResponse.has("data")) {
            writer.write(this.taskResponse.get("data").toString());
        }
        writer.close();
        System.out.println("Completed data write to json file.");
    }
}

/**
 * HMS Geometry object
 */
class Geometry{
    private int comID;
    private String stationID;
    private Point point;
    private String type;

    public int getComID(){
        return this.comID;
    }
    public void setComID(int comID){
        this.comID = comID;
        this.type = "comID";
    }
    public String getStationID(){
        return this.stationID;
    }
    public void setStationID(String stationID){
        this.stationID = stationID;
        this.type = "stationID";
    }
    public Point getPoint(){
        return this.point;
    }
    public void setPoint(double lat, double lng){
        this.point = new Point(lat, lng);
        this.type = "point";
    }
    public String getType(){
        return this.type;
    }
}


/**
 * HMS Geometry Point object
 */
class Point{
    public double latitude;
    public double longitude;
    public Point(double lat, double lng){
        this.latitude = lat;
        this.longitude = lng;
    }
}

/**
 * HMS DateTimeSpan object
 */
class DateTimeSpan{
    private String startDate;
    private String endDate;
    public DateTimeSpan(String start, String end){
        this.startDate = start;
        this.endDate = end;
    }
    public String getStartDate(){
        return this.startDate;
    }
    public String getEndDate(){
        return this.endDate;
    }
}

/**
 * HMS API POST request body object
 */
class Request{
    private String source;
    private DateTimeSpan dateTimeSpan;
    private Geometry geometry;
    private String temporalResolution;

    /**
     * Constructor for request body using comID
     * @param source data source
     * @param startDate timeseries start date
     * @param endDate timeseries end date
     * @param comID stream/catchment comID
     * @param timestep timeseries temporal resolution
     */
    public Request(String source, String startDate, String endDate, int comID, String timestep){
        this.source = source;
        this.dateTimeSpan = new DateTimeSpan(startDate, endDate);
        this.geometry = new Geometry();
        this.geometry.setComID(comID);
        this.temporalResolution = timestep;
    }

    /**
     * Constructor for request body using NCEI stationID
     * @param source data source
     * @param startDate timeseries start date
     * @param endDate timeseries end date
     * @param stationID NCEI station ID
     * @param timestep timeseries temporal resolution
     */
    public Request(String source, String startDate, String endDate, String stationID, String timestep){
        this.source = source;
        this.dateTimeSpan = new DateTimeSpan(startDate, endDate);
        this.geometry = new Geometry();
        this.geometry.setStationID(stationID);
        this.temporalResolution = timestep;
    }

    /**
     * Constructor for request body using lat/lng point
     * @param source data source
     * @param startDate timeseries start date
     * @param endDate timeseries end date
     * @param latitude point latitude value
     * @param longitude point longitude value
     * @param timestep timeseries temporal resolution
     */
    public Request(String source, String startDate, String endDate, double latitude, double longitude, String timestep){
        this.source = source;
        this.dateTimeSpan = new DateTimeSpan(startDate, endDate);
        this.geometry = new Geometry();
        this.geometry.setPoint(latitude, longitude);
        this.temporalResolution = timestep;
    }

    /**
     * Serialize request object to json string
     * @return
     */
    public String toString(){
        String requestString = "{\"source\":\"" + this.source + "\",\"dateTimeSpan\":{\"startDate\":\"" +
                this.dateTimeSpan.getStartDate() + "\",\"endDate\":\"" + this.dateTimeSpan.getEndDate() +
                "\"},\"geometry\":";
        if(this.geometry.getType().equals("point")){
            requestString += "{\"point\":{\"latitude\": " + this.geometry.getPoint().latitude +
                    ",\"longitude\":" + this.geometry.getPoint().longitude + "}";
        }
        else if(this.geometry.getType().equals("comID")) {
            requestString += "{\"comID\":" + this.geometry.getComID() + "}";
        }
        else if(this.geometry.getType().equals("stationID")) {
            requestString += "{\"stationID\":\"" + this.geometry.getStationID() + "\"}";
        }
        requestString += "},\"temporalResolution\":\"" + this.temporalResolution + "\"}";
        return requestString;
    }

}
