package com.jsrd.talk.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Chat implements Serializable, Comparable<Chat> {

    private String chatID;
    private String receiversUID;
    private String receiversNumber;
    private String sendersNumber;
    private List<Message> messages;

    public Chat() {
    }

    public Chat(String chatID, String receiversUID, String receiversNumber, String sendersNumber) {
        this.chatID = chatID;
        this.receiversUID = receiversUID;
        this.receiversNumber = receiversNumber;
        this.sendersNumber = sendersNumber;
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

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int compareTo(Chat chat) {
        return getMessages().get(getMessages().size() - 1).getDateTime().compareTo(chat.getMessages().get(chat.getMessages().size() - 1).getDateTime());
    }
}
