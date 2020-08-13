package com.jsrd.talk.model;

import java.util.List;

public class User {
    private String userID;
    private String userNumber;
    private String status;
    private String profilePic;
    private List<Chat> chatList;

    public User(String userID, String userNumber, String status, String profilePic) {
        this.userID = userID;
        this.userNumber = userNumber;
        this.status = status;
        this.profilePic = profilePic;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserNumber() {
        return userNumber;
    }

    public void setUserNumber(String userNumber) {
        this.userNumber = userNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public List<Chat> getChatList() {
        return chatList;
    }

    public void setChatList(List<Chat> chatList) {
        this.chatList = chatList;
    }
}
