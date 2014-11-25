package com.PanoramioCrawler;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.Properties;
import java.util.Scanner;


public class Crawler {
    private static Integer duplicateCount;
    private static final String url = "http://www.panoramio.com/map/get_panoramas.php";
    private static final String charset = "UTF-8";
    private static Double[] totalAreaBoundingBox = new Double[4];
    private static final Double INCREMENT_COARSE = 0.1;
    private static Connection dbConnection = null;
    private static PreparedStatement preparedStatement = null;
    private static Integer apiHitsCount =0;
    private static final double EPSILON = 0.00000001;
    private static final double INCREMENT_THRESHOLD = 0.001;
    private static String dbUserName = null;
    private static String dbPassword = null;
    private static String dbName = null;
    private static String dbTableName = null;


/*
    private static void addPOJOPhoto2CsvFile(POJOPhoto photoObject){
        String csvFileName = new String("./out/testPhoto.csv");
        try {
            FileWriter writer = new FileWriter(csvFileName, true);
            CSVFormat csvFormat = CSVFormat.EXCEL.withDelimiter(',');
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(photoObject.toArrayList());

            csvPrinter.flush();
            csvPrinter.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
*/
    private static void addPOJOPhoto2Postgres(POJOPhoto photoObject, PreparedStatement preparedStatement){

        try {
            preparedStatement.setInt(1, photoObject.getPhoto_id());
            preparedStatement.setDouble(2, photoObject.getLongitude());
            preparedStatement.setDouble(3, photoObject.getLatitude());
            preparedStatement.setString(4, photoObject.getPhoto_title());
            preparedStatement.setString(5, photoObject.getPhoto_url());
            preparedStatement.setString(6, photoObject.getPhoto_file_url());
            preparedStatement.setDate(7, photoObject.generateSQLDate());
            preparedStatement.setInt(8, photoObject.getWidth());
            preparedStatement.setInt(9, photoObject.getHeight());
            preparedStatement.setString(10, photoObject.getOwner_name());
            preparedStatement.setString(11, photoObject.getOwner_url());
            preparedStatement.setInt(12, photoObject.getOwner_id());
            preparedStatement.setString(13,photoObject.getPlace_id());

            preparedStatement.executeUpdate();

        } catch (SQLException e){
            if (e.getSQLState().equals("23505") ){
                duplicateCount++;
            }
            else
                e.printStackTrace();
        }

    }


    //TODO: think about the correct return values
    private static String generateQueryString(String p_set,Integer p_from, Integer p_to, Double[] BoundingBox , String p_mapfiler){
        //See reference here http://www.panoramio.com/api/data/api.html

        String queryString;
        try {
            queryString = String.format("set=%s&from=%s&to=%s&minx=%s&miny=%s&maxx=%s&maxy=%s&mapfilter=%s",
                    URLEncoder.encode(p_set, charset),
                    URLEncoder.encode(p_from.toString(), charset),
                    URLEncoder.encode(p_to.toString(), charset),
                    URLEncoder.encode(BoundingBox[0].toString(), charset),
                    URLEncoder.encode(BoundingBox[1].toString(), charset),
                    URLEncoder.encode(BoundingBox[2].toString(), charset),
                    URLEncoder.encode(BoundingBox[3].toString(), charset),
                    URLEncoder.encode(p_mapfiler, charset)
            );


            return queryString;
            } catch (UnsupportedEncodingException e){
            e.printStackTrace();
        }


        return "wrong";
    }

    /*
    public static void generateCSVwithHeader(){
          try {
              FileWriter writer = new FileWriter("./out/testPhoto.csv");
              CSVFormat csvFormat = CSVFormat.EXCEL.withDelimiter(',');

              CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);
              csvPrinter.printRecord("PhotoID","Latitude","Longitude","Title","PhotoURL","PhotoFileURL",
                      "UploadDate","PhotoWidth","PhotoHeight","OwnerName","OwnerURL","OwnerID","Place_ID");

              csvPrinter.flush();
              csvPrinter.close();

          } catch (IOException e){
              e.printStackTrace();
          }
    }
    */

    private static void connect2Postgres(){
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e){
            e.printStackTrace();
            System.out.println("Class org.postgresql.Driver has not been found!");
            System.exit(-1);
        }

        String dbURL = "jdbc:postgresql://localhost:5432/"+dbName;
        Properties props = new Properties();
        props.setProperty("user",dbUserName);
        props.setProperty("password",dbPassword);

        //TODO: check if SSL is needed here.
        try {
            dbConnection = DriverManager.getConnection(dbURL, props);
            preparedStatement = dbConnection.prepareStatement("INSERT INTO "+dbTableName+" VALUES(?,ST_MakePoint(?,?),?,?,?,?,?,?,?,?,?,?)");
        }catch (SQLException e){
            e.printStackTrace();
            System.out.println("The connection to Postgres database failed!");
            System.exit(-2);
        }
    }

    private static boolean biggerThan(Double d1, Double d2){
        return d1 - d2 > EPSILON;
    }


    private static void smartQueryBB(Double[] areaBoundingBox, Double increment){
        Double[] currentBoundingBox = {areaBoundingBox[0],areaBoundingBox[1],areaBoundingBox[0]+increment,areaBoundingBox[1]+increment};

        while(!biggerThan(currentBoundingBox[2],areaBoundingBox[2])){
            while(!biggerThan(currentBoundingBox[3],areaBoundingBox[3])){
                try {
                    //Set the initial queryString
                    URL queryURL = new URL(url + "?" + generateQueryString("full",0,100,currentBoundingBox,"false"));
                    URLConnection connection = queryURL.openConnection();
                    connection.setRequestProperty("Accept-Charset", charset);
                    InputStream inputStream = connection.getInputStream();
                    apiHitsCount++;

                    System.out.println("The current bounding box is: "+currentBoundingBox[0].toString()+","+currentBoundingBox[1].toString()+":"+currentBoundingBox[2].toString()+","+currentBoundingBox[3].toString());
                    System.out.println("Current increment is: "+increment);
                    //Parsing JSON using Jackson's JSON Processor
                    ObjectMapper mapper = new ObjectMapper();


                    try {

                        //Use tree model to traverse the node
                        JsonNode root = mapper.readTree(inputStream);
                        JsonNode count = root.get("count");
                        JsonNode photos = root.get("photos");
                        JsonNode has_more = root.get("has_more");

                        System.out.println("Total count of photo: "+ count.toString());

                        //If count is zero, don't bother to query the api, go directly to next grid.
                        if (count.asInt() ==0||!photos.iterator().hasNext()){
                            //Increase the Y coordinate.
                            System.out.println("No photo in this grid! Go to next grid.");
                            currentBoundingBox[3]+= increment;
                            currentBoundingBox[1]+= increment;
                            continue;
                        }


                        //If all of the following criteria are met, we do recursive call of this function fot the current grid using a fine increment:
                        /*
                            1. count is bigger than 1000,
                            2. has more than 100 records in current return
                            3. the current increment/resolution is still bigger than the threshold.
                        */
                        if (count.asInt()>1000&&has_more.asBoolean()&&biggerThan(increment,INCREMENT_THRESHOLD)){
                            System.out.println("Decompose current grid into bigger scale!");
                            smartQueryBB(currentBoundingBox, increment/2);

                            currentBoundingBox[3]+= increment;
                            currentBoundingBox[1]+= increment;
                            continue;
                        }

                        //If the count is small enough
                        Integer totalIteration=1+count.asInt()/100;

                        for (Integer i =0; i<totalIteration; i++){
                            duplicateCount =0;

                            connection = new URL(url + "?" + generateQueryString("full",i*100,(i+1)*100-1,currentBoundingBox,"false")).openConnection();
                            connection.setRequestProperty("Accept-Charset", charset);
                            inputStream = connection.getInputStream();
                            apiHitsCount++;

                            root = mapper.readTree(inputStream);
                            has_more = root.get("has_more");

                            photos = root.get("photos");

                            System.out.println("If the current set has more: "+has_more.toString());

                            for (JsonNode node: photos){
                                POJOPhoto photo = mapper.readValue(node.toString(), POJOPhoto.class);

                                //Add the entry to Postgres database
                                addPOJOPhoto2Postgres(photo, preparedStatement);
                            }

                            System.out.println("Retrieved photos from "+i*100 + " to "+((i+1)*100-1));
                            System.out.println("Number of duplicate records thrown: " + duplicateCount.toString());
                            if (!has_more.asBoolean()){
                                break;
                            }
                        }

                    } catch (JsonParseException e){
                        e.printStackTrace();
                        System.out.println("Parsing JSON failed!");
                    } catch (JsonMappingException e){
                        e.printStackTrace();
                    }

                }   catch (MalformedURLException e){
                    e.printStackTrace();
                }   catch (IOException e ){
                    System.out.println("The connection to Panoramio breaks! Can't get data from Panoramio! ");
                    System.out.println("The times we hit Panoramio API: "+apiHitsCount.toString());
                }
                System.out.println("\n");
                //Increase the Y coordinate.
                currentBoundingBox[3]+= increment;
                currentBoundingBox[1]+= increment;


            }

            currentBoundingBox[1] = areaBoundingBox[1];
            currentBoundingBox[3] = areaBoundingBox[1]+increment;

            currentBoundingBox[0]+= increment;
            currentBoundingBox[2]+= increment;

        }
    }

    private static void closePostgres(){
        try{
            preparedStatement.close();
            dbConnection.close();
        }catch (SQLException e){
            System.out.println("Closing Postgres failed!");
        }
    }

    private static void promptUserInput(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the name of the Postgres Database: ");
        dbName = scanner.nextLine();
        System.out.println("Enter the name of the table: ");
        dbTableName = scanner.nextLine();
        System.out.println("Enter the user name of the database: ");
        dbUserName = scanner.nextLine();
        System.out.println("Enter the password of this user name: ");
        dbPassword = scanner.nextLine();
        System.out.println("Enter the bounding box of the area to download Panoramio photos " +
                "in the format of minx, miny, maxx, maxy: ");
        String[] BBox = scanner.nextLine().split(",");
        for (int i=0;i<4;i++)
            totalAreaBoundingBox[i] = Double.parseDouble(BBox[i]);
    }

    public static void main(String[] args) {

        promptUserInput();

        //Create connection to Postgres
        connect2Postgres();

        try{
            smartQueryBB(totalAreaBoundingBox, INCREMENT_COARSE);
        } finally{
            closePostgres();
        }
    }
}
