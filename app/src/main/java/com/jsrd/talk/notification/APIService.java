package com.jsrd.talk.notification;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    String serverKey = "key=" + "AAAA28DHfp4:APA91bE-qpbOOCU2fHR_JQCPI_Kbs4Q7SxSx4ezyQr1BnciWpw7eRM4jpNjN-nm1jlzNWfJra3T5PtJ8v20PPXXqYf6CxtKTqIXtzONrIPkHMmNOjl5yiG8kN53qKzc37VPRibLPDwOK";

    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:" + serverKey
            }
    )
    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);

}
