package com.xploreict.quizappadmin;

import java.util.List;

public class CategoriesModel {
    private String name;
    private List<String> sets;
    private String url;
    private String key;

    public CategoriesModel(){
        //for firebase
    }

    public CategoriesModel(String name, List<String> sets, String url, String key) {
        this.name = name;
        this.sets = sets;
        this.url = url;
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSets() {
        return sets;
    }

    public void setSets(List<String> sets) {
        this.sets = sets;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
