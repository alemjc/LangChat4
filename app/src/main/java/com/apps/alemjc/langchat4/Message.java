package com.apps.alemjc.langchat4;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by alemjc on 10/2/16.
 */
public class Message {

    private String by;
    private String message;
    private String fromLang;

    public Message(){}

    public Message(String by, String message, String fromLang){
        this.by = by;
        this.message = message;
        this.fromLang = fromLang;
    }

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFromLang() {
        return fromLang;
    }

    public void setFromLang(String fromLang) {
        this.fromLang = fromLang;
    }

    @Exclude
    public Map<String, Object> toMap(){
        HashMap<String, Object> map = new HashMap<>();
        map.put("by",getBy());
        map.put("message",getMessage());
        map.put("fromLang",getFromLang());

        return map;
    }
}
