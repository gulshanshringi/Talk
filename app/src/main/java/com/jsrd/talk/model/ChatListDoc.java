package com.jsrd.talk.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ChatListDoc {

    public List<Chat> chatList;

    public ChatListDoc(List<Chat> chatList) {
        this.chatList = chatList;
    }

    public ChatListDoc() {



    }
}
