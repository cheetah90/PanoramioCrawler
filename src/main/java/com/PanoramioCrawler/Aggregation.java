package com.PanoramioCrawler;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;

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

    public static void generateCSVwithHeader(){
        try {
            FileWriter writer = new FileWriter("./out/testPhoto.csv");
            CSVFormat csvFormat = CSVFormat.EXCEL.withDelimiter(',');

            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);
            csvPrinter.printRecord("CountyFIPS","PhotoCounts");

            csvPrinter.flush();
            csvPrinter.close();

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static void addRow2Csv(String FIPS, Integer photoCounts){
        String csvFileName = new String("./out/testPhoto.csv");
        try {
            FileWriter writer = new FileWriter(csvFileName, true);
            CSVFormat csvFormat = CSVFormat.EXCEL.withDelimiter(',');
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            ArrayList<String> arrayList = new ArrayList<String>();
            arrayList.add(FIPS);
            arrayList.add(photoCounts.toString());


            csvPrinter.printRecord(arrayList);

            csvPrinter.flush();
            csvPrinter.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }



    private static void connect2Postgres(){
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


        FeatureSource areaUnitsFS= null;
        FeatureSource UgcFS = null;

        try {
            areaUnitsFS = postGISDataStore.getFeatureSource(dbAggreUnitTableName);
            UgcFS = postGISDataStore.getFeatureSource(dbUGCTableName);
            System.out.println("bc count: " + areaUnitsFS.getCount(Query.ALL));
            System.out.println("bc count: " + UgcFS.getCount(Query.ALL));

            Filter pointInPolygon = null;
            FeatureCollection photoFC = null;
            String countyUID = null;
            FeatureIterator countiesIterator = areaUnitsFS.getFeatures().features();
            generateCSVwithHeader();

            try {
                while(countiesIterator.hasNext() ){
                    SimpleFeature countyFeature = (SimpleFeature) countiesIterator.next();
                    Geometry countyGeometry = (Geometry) countyFeature.getDefaultGeometry();


                    pointInPolygon = CQL.toFilter("WITHIN(geom,"+countyGeometry.toString()+")");
                    photoFC = UgcFS.getFeatures(pointInPolygon);

                    countyUID = (String) countyFeature.getAttribute("fips_str");

                    addRow2Csv(countyUID, photoFC.size());

                }
            } catch (CQLException e){
                e.printStackTrace();
            }
            finally {
                countiesIterator.close();
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
