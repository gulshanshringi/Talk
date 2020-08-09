package com.jsrd.talk;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.interfaces.ProgressSuccessCallBack;
import com.jsrd.talk.interfaces.ReceiverCallback;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.ChatListDoc;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.model.MessageListDoc;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseUtils {

    private final String TAG = "FirebaseUtils";
    private FirebaseFirestore db;
    private Context mContext;

    public FirebaseUtils(Context context) {
        mContext = context;
        db = FirebaseFirestore.getInstance();
    }

    public String getCurrentUserUID() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String UID = user.getUid();
        return UID;
    }

    public String getCurrentUserNumber() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String number = user.getPhoneNumber();
        return number;
    }

    public void putUserInfoOnFirestore(FirebaseUser user) {
        String userUID = getCurrentUserUID();
        Map<String, Object> userData = new HashMap<>();
        userData.put("Number", user.getPhoneNumber());
        userData.put("UID", userUID);


        Map<String, Object> adduserToList = new HashMap<>();
        adduserToList.put("UsersList", FieldValue.arrayUnion(userData));

        db.collection("Users").document("list")
                .update(adduserToList).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //  callBack.onSuccess(true);
            }
        });

    }

    public void getReceiversUID(final String number, final ReceiverCallback callBack) {
        db.collection("Users").document("list").get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    boolean isNumIsRegistered = false;
                    String UID = null;
                    DocumentSnapshot document = task.getResult();
                    List<Map<String, Object>> usersList = (List<Map<String, Object>>) document.get("UsersList");

                    for (Map<String, Object> user : usersList) {
                        String num = (String) user.get("Number");
                        if (num.contains(number) || num.equals(number)) {
                            isNumIsRegistered = true;
                            UID = (String) user.get("UID");
                            break;
                        }
                    }
                    if (isNumIsRegistered) {
                        callBack.onComplete(UID);
                    } else {
                        callBack.onComplete(null);
                    }
                } else {
                    callBack.onComplete(null);
                }
            }
        });
    }

    public void sendMessage(final String receiversNumber, String sendersNumber, final String message, final String chatID, final ChatCallBack callBack) {
        String userUID = getCurrentUserUID();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy_HH:mm:ss", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());

        List<Message> messageList = new ArrayList<>();
        Message messageData = new Message(message, userUID, currentDateandTime);


        if (chatID == null) {
            messageList.add(messageData);
            MessageListDoc messageListDoc = new MessageListDoc(messageList);

            db.collection("Chats").
                    add(messageListDoc).
                    addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            final String chatId = documentReference.getId();

                            if (chatId.length() > 0) {
                                getReceiversUID(receiversNumber, new ReceiverCallback() {
                                    @Override
                                    public void onComplete(String UID) {
                                        putChatDataOnFirebaseForSender(receiversNumber, sendersNumber, UID, chatId, new ProgressSuccessCallBack() {
                                            @Override
                                            public void onSuccess(boolean success) {
                                                callBack.onComplete(chatId, null, null);
                                            }
                                        });
                                    }
                                });


                            }
                        }
                    });
        } else {
            Map<String, Object> addMsgToChat = new HashMap<>();
            addMsgToChat.put("messageList", FieldValue.arrayUnion(messageData));

            db.collection("Chats").document(chatID)
                    .update(addMsgToChat).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    callBack.onComplete(chatID, null, null);
                }
            });
        }
    }

    public void putChatDataOnFirebaseForSender(final String receiversNumber, String sendersNumber, String receiversUID, String chatId, final ProgressSuccessCallBack callBack) {
        final String userUID = getCurrentUserUID();

        if (chatId.length() > 0) {
            Chat chatData = new Chat(chatId, receiversUID, receiversNumber, sendersNumber);

            getUsersChatList(userUID, new ChatCallBack() {
                @Override
                public void onComplete(String chatID, List<Message> messageList, List<Chat> chatList) {
                    if (chatList == null) {
                        chatList = new ArrayList<>();
                        chatList.add(chatData);
                        ChatListDoc chatListDoc = new ChatListDoc(chatList);

                        db.collection("Users").
                                document(userUID).
                                set(chatListDoc).
                                addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        putChatDataOnFirebaseForReceiver(receiversUID, receiversNumber, chatId, new ProgressSuccessCallBack() {
                                            @Override
                                            public void onSuccess(boolean success) {
                                                callBack.onSuccess(true);
                                            }
                                        });

                                    }
                                });
                    } else {
                        Map<String, Object> addChatDataToList = new HashMap<>();
                        addChatDataToList.put("ChatList", FieldValue.arrayUnion(chatData));

                        db.collection("Users").
                                document(userUID)
                                .update(addChatDataToList).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                callBack.onSuccess(true);
                            }
                        });

                    }
                }
            });


        }
    }

    public void putChatDataOnFirebaseForReceiver(String receiversUID, String receiversNumber, String chatId, final ProgressSuccessCallBack callBack) {
        final String userUID = getCurrentUserUID();
        String userNumber = getCurrentUserNumber();

        if (chatId.length() > 0) {
            Chat chatData = new Chat(chatId, userUID, userNumber, receiversNumber);

            getUsersChatList(receiversUID, new ChatCallBack() {
                @Override
                public void onComplete(String chatID, List<Message> messageList, List<Chat> chatList) {
                    if (chatList == null) {
                        chatList = new ArrayList<>();
                        chatList.add(chatData);
                        ChatListDoc chatListDoc = new ChatListDoc(chatList);

                        db.collection("Users").
                                document(receiversUID).
                                set(chatListDoc).
                                addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        callBack.onSuccess(true);
                                    }
                                });
                    } else {
                        Map<String, Object> addChatDataToList = new HashMap<>();
                        addChatDataToList.put("ChatList", FieldValue.arrayUnion(chatData));

                        db.collection("Users").
                                document(receiversUID)
                                .update(addChatDataToList).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                callBack.onSuccess(true);
                            }
                        });

                    }
                }
            });


        }
    }

    public void getUsersChatList(String userUID, final ChatCallBack callBack) {
        db.collection("Users").
                document(userUID).
                get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Chat> chatList = new ArrayList<>();
                            DocumentSnapshot document = task.getResult();
                            if (document.toObject(ChatListDoc.class) != null) {
                                chatList = document.toObject(ChatListDoc.class).ChatList;
                                if (chatList != null) {
                                    callBack.onComplete(null, null, chatList);
                                } else {
                                    callBack.onComplete(null, null, null);
                                }
                            } else {
                                callBack.onComplete(null, null, null);
                            }
                        } else {
                            callBack.onComplete(null, null, null);
                        }
                    }
                });
    }

    public void getChatData(final String chatId, final ChatCallBack callBack) {
        if (chatId != null) {
            db.collection("Chats").
                    document(chatId).
                    get().
                    addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();

                                if (document.toObject(MessageListDoc.class) != null) {
                                    List<Message> messageList = document.toObject(MessageListDoc.class).messageList;
                                    callBack.onComplete(chatId, messageList, null);
                                } else {
                                    callBack.onComplete(null, null, null);
                                }
                            } else {
                                callBack.onComplete(null, null, null);
                            }
                        }
                    });
        } else {
            callBack.onComplete(null, null, null);
        }
    }

    public void listenForMessagesInRealTime(String chatId, final ChatCallBack callBack) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        final DocumentReference docRef = db.collection("Chats").document(chatId);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ChatActivity", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists() && snapshot.toObject(MessageListDoc.class) != null) {
                    List<Message> messageList = snapshot.toObject(MessageListDoc.class).messageList;
                    if (messageList != null) {
                        callBack.onComplete(null, messageList, null);
                    } else {
                        callBack.onComplete(null, null, null);
                    }
                } else {
                    callBack.onComplete(null, null, null);
                }
            }
        });
    }

    public void listenForChatsInRealTime(final ChatCallBack callBack) {
        String userUID = getCurrentUserUID();

        DocumentReference docRef = db.collection("Users").document(userUID);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("ChatActivity", "Listen failed.", e);
                    return;
                }
                if (snapshot != null && snapshot.exists() && snapshot.toObject(ChatListDoc.class) != null) {
                    List<Chat> chatList = snapshot.toObject(ChatListDoc.class).ChatList;
                    if (chatList != null) {
                        callBack.onComplete(null, null, chatList);
                    } else {
                        callBack.onComplete(null, null, null);
                    }
                } else {
                    callBack.onComplete(null, null, null);
                }
            }
        });
    }

}
