package com.jsrd.talk.interfaces;

import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.Message;

import java.util.List;

public interface ChatCallBack {
    void onComplete(String chatID, List<Message> messageList, List<Chat> chatList);

}
