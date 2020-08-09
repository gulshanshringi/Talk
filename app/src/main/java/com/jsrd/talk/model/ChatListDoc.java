package com.jsrd.talk.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChatListDoc {

    public List<Chat> ChatList;

    public ChatListDoc(List<Chat> ChatList) {
        this.ChatList = ChatList;
    }

    public ChatListDoc() {



    }
}
