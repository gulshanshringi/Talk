package com.jsrd.talk.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.jsrd.talk.interfaces.ChatCallBack;
import com.jsrd.talk.interfaces.ImageUploadCallBack;
import com.jsrd.talk.interfaces.ProgressSuccessCallBack;
import com.jsrd.talk.interfaces.ReceiverCallback;
import com.jsrd.talk.interfaces.StatusCallBack;
import com.jsrd.talk.model.Chat;
import com.jsrd.talk.model.ChatListDoc;
import com.jsrd.talk.model.Message;
import com.jsrd.talk.model.MessageListDoc;
import com.jsrd.talk.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jsrd.talk.utils.Utils.getCurrentDateAndTime;

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
        if (user != null) {
            String UID = user.getUid();
            return UID;
        } else {
            return null;
        }
    }

    public String getCurrentUserNumber() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String number = user.getPhoneNumber();
        return number;
    }

    public void putUserInfoOnFirestore(FirebaseUser firebaseUser) {
        String userID = getCurrentUserUID();
        String profilePic = firebaseUser.getPhotoUrl().toString();
        User user = new User(userID, firebaseUser.getPhoneNumber(), "online", profilePic);

        Map<String, Object> adduserToList = new HashMap<>();
        adduserToList.put("UsersList", FieldValue.arrayUnion(user));

        db.collection("Users").document("list")
                .update(adduserToList).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //  callBack.onSuccess(true);
            }
        });

        db.collection("Users").
                document(userID).
                set(user);
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
                        String num = (String) user.get("userNumber");
                        if (num.contains(number) || num.equals(number)) {
                            isNumIsRegistered = true;
                            UID = (String) user.get("userID");
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

    public void getReceiversProfilePic(final String number, final ReceiverCallback callBack) {
        db.collection("Users").
                document("list").
                get().
                addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            boolean isNumIsRegistered = false;
                            String profilePic = null;
                            DocumentSnapshot document = task.getResult();
                            List<Map<String, Object>> usersList = (List<Map<String, Object>>) document.get("UsersList");

                            for (Map<String, Object> user : usersList) {
                                String num = (String) user.get("userNumber");
                                if (num.contains(number) || num.equals(number)) {
                                    isNumIsRegistered = true;
                                    profilePic = (String) user.get("profilePic");
                                }
                            }
                            if (isNumIsRegistered && profilePic != null) {
                                callBack.onComplete(profilePic);
                            } else {
                                callBack.onComplete(null);
                            }
                        } else {
                            callBack.onComplete(null);
                        }
                    }
                });
    }


    public void sendMessage(final String receiversNumber, String sendersNumber,
                            final String message, final String chatID, final ChatCallBack callBack) {
        String userUID = getCurrentUserUID();
        String currentDateandTime = getCurrentDateAndTime();

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

    public void putChatDataOnFirebaseForSender(final String receiversNumber, String sendersNumber,
                                               String receiversUID, String chatId, final ProgressSuccessCallBack callBack) {
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
                        addChatDataToList.put("chatList", FieldValue.arrayUnion(chatData));

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
                        addChatDataToList.put("chatList", FieldValue.arrayUnion(chatData));

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
                                chatList = document.toObject(ChatListDoc.class).chatList;
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
                    List<Chat> chatList = snapshot.toObject(ChatListDoc.class).chatList;
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

    public void updateStatus(String status) {
        String userID = getCurrentUserUID();
        if (userID != null) {
            db.collection("Users").document(userID).update("status", status);
        }
    }

    public void listenForStatusInRealTime(String userID, StatusCallBack callBack) {
        db.collection("Users").document(userID).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (value != null) {
                    String status = (String) value.get("status");
                    if (status != null) {
                        callBack.onStatusUpdate(status);
                    }
                }
            }
        });
    }


    public void markedMessagesAsSeen(List<Message> messageList, String chatID) {
        if (chatID != null) {
            Log.i("ChatAdapter", "marked");
            MessageListDoc messageListDoc = new MessageListDoc(messageList);

            db.collection("Chats").document(chatID).set(messageListDoc);
        }
    }

    public void uploadImageToFirestore(Uri imageUri, ImageUploadCallBack callBack) {
        Log.i(TAG, "Image Uri received for upload");
        StorageReference storageReference = FirebaseStorage.getInstance().getReference("Images");
        StorageReference fileReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(imageUri));

        UploadTask uploadTask = fileReference.putFile(imageUri);

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.i(TAG, "Image uploaded to firebase storage");
                    fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            callBack.onImageUpload(uri);
                        }
                    });
                }
            }
        });


    }

    private String getFileExtension(Uri uri) {
        ContentResolver cr = mContext.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();

        return mime.getExtensionFromMimeType(cr.getType(uri));
    }
}
