package com.example.simplenotesapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Note {
    private String id;
    private long timestamp;
    private String text;

    public Note() {
        // Required for Firebase
    }

    public Note(long timestamp, String text) {
        this.timestamp = timestamp;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String dateStr = fmt.format(new Date(timestamp));
        return dateStr + "\n" + (text != null ? text : "");
    }
}
