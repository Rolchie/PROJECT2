package com.maramagagriculturalaid.app.Municipal.BarangayOverview;

public class Barangay {
    private String name;
    private int farmersCount;
    private String location;

    public Barangay(String name, int farmersCount, String location) {
        this.name = name;
        this.farmersCount = farmersCount;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public int getFarmersCount() {
        return farmersCount;
    }

    public String getLocation() {
        return location;
    }
}
