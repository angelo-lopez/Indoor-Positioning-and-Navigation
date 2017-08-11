package com.angelsoft.angelo_romel.indoorpositioning;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Location extends RealmObject {

    @PrimaryKey//sets id to be the primary key of the Location object of "location_realm" database.
    private String id;
    private String type;
    private double lat;
    private double lon;
    private String floorId;
    private String block;
    private int level;
    private String buildingName;

    //default constructor
    public Location() {}

    //custom constructor
    public Location(String id, String type, double lat, double lon, String floorId,
                    String block, int level, String buildingName) {
        this.id = id;
        this.type = type;
        this.lat = lat;
        this.lon = lon;
        this.floorId = floorId;
        this.block = block;
        this.level = level;
        this.buildingName = buildingName;
    }

    //standard getters
    public String getId(){
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public double getLat() {
        return this.lat;
    }

    public double getLon() {
        return this.lon;
    }

    public String getFloorId() {
        return this.floorId;
    }

    public String getBlock() {
        return this.block;
    }

    public int getLevel() {
        return this.level;
    }

    public String getBuildingName() {
        return this.buildingName;
    }

    //standard setters
    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public void setFloorId(String floorId) {
        this.floorId = floorId;
    }

    public void setBlock(String block) {
        this.block = block;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

}
