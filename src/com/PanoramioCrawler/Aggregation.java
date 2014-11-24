package com.PanoramioCrawler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;

/**
 * Created by allenlin on 11/24/14.
 */
public class Aggregation {
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

    private static void promptUserInput(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the Postgres Database: ");
        dbName = scanner.nextLine();
        System.out.println("Enter the table: ");
        dbTableName = scanner.nextLine();
        System.out.println("Enter the user name: ");
        dbUserName = scanner.nextLine();
        System.out.println("Enter the password: ");
        dbPassword = scanner.nextLine();
    }

    public static void main(String[] args){
        promptUserInput();
        connect2Postgres();
    }
}
