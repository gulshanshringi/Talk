package com.jsrd.talk.interfaces;

import com.jsrd.talk.model.Chat;

import java.util.List;

public interface ChatListCallBack {
    void onComplete(List<Chat> chatList);
}
