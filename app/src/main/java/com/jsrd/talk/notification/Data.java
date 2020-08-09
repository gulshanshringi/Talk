package com.jsrd.talk.notification;

public class Data {

    private String body;
    private String title;
    private String chatID;
    private String userID;

    public Data(String body, String title, String chatID, String userID) {
        this.body = body;
        this.title = title;
        this.chatID = chatID;
        this.userID = userID;
    }

    public String getBody() {
        return body;
    }

    public String getTitle() {
        return title;
    }

    public String getChatID() {
        return chatID;
    }

    public String getUserID() {
        return userID;
    }
}
