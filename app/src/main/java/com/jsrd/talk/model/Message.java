package com.jsrd.talk.model;

import android.os.Parcelable;

import java.io.Serializable;

public class Message implements Serializable {

    private String dateTime;
    private String sender;
    private String message;
    private boolean seen = false;

    public Message() {
        //default constructor
    }

    public Message(String message, String sender, String dateTime) {
        this.dateTime = dateTime;
        this.sender = sender;
        this.message = message;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}
