package com.jsrd.talk.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

public class Chat implements Serializable, Comparable<Chat> {

    private String chatID;
    private String receiversUID;
    private String receiversNumber;
    private String sendersNumber;
    private String dateTime;
    private List<Message> messages;

    public Chat() {
    }

    public Chat(String chatID, String receiversUID, String receiversNumber, String sendersNumber, String dateTime) {
        this.chatID = chatID;
        this.receiversUID = receiversUID;
        this.receiversNumber = receiversNumber;
        this.sendersNumber = sendersNumber;
        this.dateTime = dateTime;
    }

    public String getChatID() {
        return chatID;
    }

    public void setChatID(String chatID) {
        this.chatID = chatID;
    }

    public String getReceiversUID() {
        return receiversUID;
    }

    public void setReceiversUID(String receiversUID) {
        this.receiversUID = receiversUID;
    }

    public String getReceiversNumber() {
        return receiversNumber;
    }

    public void setReceiversNumber(String receiversNumber) {
        this.receiversNumber = receiversNumber;
    }

    public String getSendersNumber() {
        return sendersNumber;
    }

    public void setSendersNumber(String sendersNumber) {
        this.sendersNumber = sendersNumber;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int compareTo(Chat chat) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss");
        try {
            return f.parse(chat.getDateTime()).compareTo(f.parse(getDateTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
