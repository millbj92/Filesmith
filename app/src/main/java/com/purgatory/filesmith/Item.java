package com.purgatory.filesmith;

import android.support.annotation.NonNull;

class Item implements Comparable<Item>{
    private String name;
    private String data;
    private String date;
    private String path;
    private String image;

    Item(String n,String d, String dt, String p, String img)
    {
        name = n;
        data = d;
        date = dt;
        path = p;
        image = img;
    }
    public String getName()
    {
        return name;
    }
    public String getData()
    {
        return data;
    }
    String getDate()
    {
        return date;
    }
    public String getPath()
    {
        return path;
    }
     String getImage() {
        return image;
    }
    void setImage(String img){image = img; }

    public int compareTo(@NonNull Item o) {
        if(this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }

    @Override
    public String toString()
    {
        return "Name: " + name + "\n" + "Data: " + data + "\n" + "Date: " + date + "\n" + "Path: " + path + "\n" + "Image: " + image;
    }
}
