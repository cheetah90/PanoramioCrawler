package com.PanoramioCrawler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.sql.Date;
import java.util.List;

/**
 * Created by allenlin on 10/28/14.
 */
public class POJOPhoto {

    //The attributes of a photo, returned by Panoramio Data API

    private Integer height;
    private Double latitude;
    private Double longitude;
    private Integer owner_id;
    private String owner_name;
    private String owner_url;
    private String photo_file_url;
    private Integer photo_id;
    private String photo_title;
    private String photo_url;
    private String upload_date;
    private Integer width;
    private String place_id;

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getOwner_id() {
        return owner_id;
    }

    public void setOwner_id(Integer owner_id) {
        this.owner_id = owner_id;
    }

    public String getOwner_name() {
        return owner_name;
    }

    public void setOwner_name(String owner_name) {
        this.owner_name = owner_name;
    }

    public String getOwner_url() {
        return owner_url;
    }

    public void setOwner_url(String owner_url) {
        this.owner_url = owner_url;
    }

    public String getPhoto_file_url() {
        return photo_file_url;
    }

    public void setPhoto_file_url(String photo_file_url) {
        this.photo_file_url = photo_file_url;
    }

    public Integer getPhoto_id() {
        return photo_id;
    }

    public void setPhoto_id(Integer photo_id) {
        this.photo_id = photo_id;
    }

    public String getPhoto_title() {
        return photo_title;
    }

    public void setPhoto_title(String photo_title) {
        this.photo_title = photo_title;
    }

    public String getPhoto_url() {
        return photo_url;
    }

    public void setPhoto_url(String photo_url) {
        this.photo_url = photo_url;
    }

    public String getUpload_date() {
        return upload_date;
    }

    public void setUpload_date(String upload_date) {
        this.upload_date = upload_date;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public String getPlace_id() {
        return place_id;
    }

    public void setPlace_id(String place_id) {
        this.place_id = place_id;
    }

    @Override
    public String toString(){
        return "Photo_ID="+photo_id+",Latitude="+latitude+",Longitude="+longitude+",Photo_Title="+photo_title+
                ",Photo_URL="+photo_url+",Photo_File_URL="+photo_file_url+",Upload_Date="+upload_date+",Width="+width+
                ",Height="+height+",Owner_Name="+owner_name+",Owner_URL="+owner_url+",Owner_ID="+owner_id+",Place_ID="+place_id+'\n';
    }

    public List<String> toArrayList(){
        List<String> attributeList = new ArrayList<String>();
        attributeList.add(getPhoto_id().toString());
        attributeList.add(getLatitude().toString());
        attributeList.add(getLongitude().toString());
        attributeList.add(getPhoto_title());
        attributeList.add(getPhoto_url());
        attributeList.add(getPhoto_file_url());
        attributeList.add(getUpload_date());
        attributeList.add(getWidth().toString());
        attributeList.add(getHeight().toString());
        attributeList.add(getOwner_name());
        attributeList.add(getOwner_url());
        attributeList.add(getOwner_id().toString());
        attributeList.add(getPlace_id());

        return attributeList;
    }


    //TODO: double check if this is usable
    /*
    public String generateSQLGEOM(){
        String sqlGEOM = String.format("ST_GeomFromText('POINT(%s %s)')",getLatitude(),getLongitude());

        return sqlGEOM;
    }
    */

    public Date generateSQLDate(){
        try {
            java.util.Date javaDate = new SimpleDateFormat("dd MMMMMMM yyyy").parse(upload_date);
            Date sqlDate = new Date(javaDate.getTime());

            return sqlDate;

        } catch (ParseException e){
            e.printStackTrace();
            return null;
        }
    }
}

