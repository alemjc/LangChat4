package com.apps.alemjc.langchat4;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import com.fasterxml.jackson.annotation.JacksonAnnotation;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alemjc on 9/6/16.
 */
public class User implements Serializable, Comparable<User>{

    private List<String> friends;
    private List<String> chats;
    private String displayName;
    @Exclude
    private static final long serialVersionUID = 422L;
    private String uid;
    private String imagePath;
    private String status;

    public User(){}

    public User(String uid, String imagePath,String displayName, String status, List<String> friends, List<String> chats){
        this.displayName = displayName;
        this.uid = uid;
        this.friends = friends;
        this.chats = chats;
        this.imagePath = imagePath;
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public List<String> getChats() {
        return chats;
    }

    public void setChats(List<String> chats) {
        this.chats = chats;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImagePath() {
        return imagePath;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonIgnore
    @Exclude
    public Map<String, Object> toMap(){
        HashMap<String, Object> map = new HashMap<>();
        map.put("displayName", getDisplayName());
        map.put("uid", getUid());
        map.put("friends", getFriends());
        map.put("chats", getChats());
        map.put("imagePath", getImagePath());
        map.put("status", getStatus());

        return map;
    }

    @Override
    @Exclude
    public int compareTo(User user) {
        return this.getUid().compareTo(user.getUid());
    }
}
