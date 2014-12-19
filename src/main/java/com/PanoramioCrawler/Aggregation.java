package com.PanoramioCrawler;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

/**
 * Created by allenlin on 11/24/14.
 */
public class Aggregation {
    private static String dbUserName = null;
    private static String dbPassword = null;
    private static String dbName = null;
    private static String dbAggreUnitTableName = null;
    private static DataStore postGISDataStore = null;
    private static String dbUGCTableName = null;
    private static Connection dbConnection = null;


    public static void generateCSVwithHeader(){
        try {
            FileWriter writer = new FileWriter("./out/output.csv");
            CSVFormat csvFormat = CSVFormat.EXCEL.withDelimiter(',');

            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);
            csvPrinter.printRecord("CountyFIPS","PhotoCounts");

            csvPrinter.flush();
            csvPrinter.close();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void addRow2Csv(ArrayList arrayList){
        String csvFileName ="./out/output.csv";
        try {
            FileWriter writer = new FileWriter(csvFileName, true);
            CSVFormat csvFormat = CSVFormat.EXCEL.withDelimiter(',');
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(arrayList);

            csvPrinter.flush();
            csvPrinter.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }



    private static void connect2Postgres(){

        /**
         * Connect via Geotool DataStore (Slow)
         */
        Map<String,Object> params = new HashMap<String,Object>();
        params.put( "dbtype", "postgis");
        params.put( "host", "localhost");
        params.put( "port", 5432);
        params.put( "schema", "public");
        params.put( "database", dbName);
        params.put( "user", dbUserName);
        params.put( "passwd", dbPassword);

        try {
            postGISDataStore = new PostgisNGDataStoreFactory().createDataStore(params);
        } catch (IOException e){
            e.printStackTrace();
        }


        /**
         * Connect via JDBC
         */

        String dbURL = "jdbc:postgresql://localhost:5432/"+dbName;
        Properties props = new Properties();
        props.setProperty("user",dbUserName);
        props.setProperty("password",dbPassword);

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e){
            e.printStackTrace();
            System.out.println("Class org.postgresql.Driver has not been found!");
            System.exit(-1);
        }

        try {
            dbConnection = DriverManager.getConnection(dbURL, props);
        }catch (SQLException e){
            e.printStackTrace();
            System.out.println("The connection to Postgres database failed!");
            System.exit(-2);
        }


    }

    private static void promptUserInput(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the Postgres Database: ");
        dbName = scanner.nextLine();
        System.out.println("Enter the user name: ");
        dbUserName = scanner.nextLine();
        System.out.println("Enter the password: ");
        dbPassword = scanner.nextLine();
        System.out.println("Enter the spatial aggregation units table: ");
        dbAggreUnitTableName = scanner.nextLine();
        System.out.println("Enter the UGC table to be summarized: ");
        dbUGCTableName = scanner.nextLine();

    }

    public static void main(String[] args){
        promptUserInput();
        connect2Postgres();


        FeatureSource areaUnitsFS;
        FeatureSource UgcFS;

        try {
            areaUnitsFS = postGISDataStore.getFeatureSource(dbAggreUnitTableName);
            UgcFS = postGISDataStore.getFeatureSource(dbUGCTableName);
            System.out.println("count of area units: " + areaUnitsFS.getCount(Query.ALL));
            System.out.println("count of ugc features: " + UgcFS.getCount(Query.ALL));

            Filter pointInPolygon;
            FeatureCollection photoFC;
            String areaUnitUID;
            ArrayList<String> arrayList = new ArrayList<String>();

            FeatureIterator countiesIterator = areaUnitsFS.getFeatures().features();
            generateCSVwithHeader();

            /**
             * Another way: directly issue query to PSQL, so we can leverage SQL aggregate function.
             */
            PreparedStatement getDistinctUsersCount = dbConnection.prepareStatement("SELECT COUNT(DISTINCT owner_id) FROM "+dbUGCTableName +" WHERE ST_WITHIN(geom, ST_GeomFromText(?,4326))");
            ResultSet rsDistinctUserCount;
            Integer count=null;

            try{
               while(countiesIterator.hasNext()){
                   SimpleFeature countyFeature = (SimpleFeature) countiesIterator.next();
                   Geometry countyGeometry = (Geometry) countyFeature.getDefaultGeometry();

                   getDistinctUsersCount.setString(1,countyGeometry.toString());
                   rsDistinctUserCount = getDistinctUsersCount.executeQuery();

                   while (rsDistinctUserCount.next())
                   {
                       count = rsDistinctUserCount.getInt(1);
                       arrayList.add(countyFeature.getAttribute("fips_str").toString());
                       //arrayList.add(countyFeature.getAttribute("province").toString());
                       //arrayList.add(countyFeature.getAttribute("prefecture").toString());
                       arrayList.add(Integer.toString(count));
                   }
                   System.out.println("distinct users:"+ count);

                   addRow2Csv(arrayList);
                   arrayList.clear();

               }
            } finally {
                countiesIterator.close();
            }

            /*
            try {
                while(countiesIterator.hasNext() ){

                    SimpleFeature countyFeature = (SimpleFeature) countiesIterator.next();
                    Geometry countyGeometry = (Geometry) countyFeature.getDefaultGeometry();


                    pointInPolygon = CQL.toFilter("WITHIN(geom,"+countyGeometry.toString()+")");
                    photoFC = UgcFS.getFeatures(pointInPolygon);

                    areaUnitUID = (String) countyFeature.getAttribute("fips_str");
                    if (!areaUnitUID.isEmpty()){
                        arrayList.add(areaUnitUID);
                        //arrayList.add(countyFeature.getAttribute("province").toString());
                        //arrayList.add(countyFeature.getAttribute("prefecture").toString());
                        arrayList.add(Integer.toString(photoFC.size()));

                        System.out.println("Current Area Unit UID is: "+areaUnitUID+"\t"+"Counts: "+Integer.toString(photoFC.size()));

                        addRow2Csv(arrayList);
                        arrayList.clear();
                    }
                    else {
                        System.out.println("The UID is empty");
                        System.exit(-1);
                    }
                }
            } catch (CQLException e){
                e.printStackTrace();
            }
            finally {
                countiesIterator.close();
            }
            */

        } catch (IOException e){
            e.printStackTrace();
        } catch (SQLException e){
            e.printStackTrace();
        }

    }
}
