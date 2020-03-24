package com.onvit.chatapp.util;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.Gson;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.NotificationModel;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Utiles {
    public static final int firstReadChatCount = 50;
    public static AlertDialog createLoadingDialog(Context context, String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.loading, null);
        TextView tx = view.findViewById(R.id.loading_text);
        tx.setText(text);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    public static void sendFcm(List<String> registration_ids, String message, Context context, String toRoom, String uri) {
        Log.d("들어오는수", "dd");
        Gson gson = new Gson();
        String userName = PreferenceManager.getString(context, "name");
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.registration_ids = registration_ids;
//        notificationModel.notification.title = userName;
//        notificationModel.notification.text = message;
//        notificationModel.notification.tag = toRoom;
//        notificationModel.notification.click_action = "GroupMessage";
        if (message.length() > 30) {
            message = message.substring(0, 30) + "...";
        }
        notificationModel.data.title = userName;
        notificationModel.data.body = message;
        notificationModel.data.tag = toRoom;
        notificationModel.data.click_action = "GroupMessage";
        notificationModel.data.uri = uri;
        notificationModel.content_available = true;
        notificationModel.priority = "high";
        notificationModel.delay_while_idle = false;
        notificationModel.time_to_live = 0;

        RequestBody requestBody = RequestBody.create(gson.toJson(notificationModel), MediaType.parse("application/json; charset=utf8"));
        Request request = new Request.Builder().header("Content-Type", "apllication/json")
                .addHeader("Authorization", "key=" + PreferenceManager.getString(context, "serverKey") + "")
                .url("https://fcm.googleapis.com/fcm/send")
                .post(requestBody)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {

            }
        });
    }
    public static Toast customToast(Activity context, String text){
        Toast toast = new Toast(context);
        View custom = context.getLayoutInflater().inflate(R.layout.custom_toast,null);
        TextView textView = custom.findViewById(R.id.message_toast);
        textView.setText(text);
        ImageView imageView = custom.findViewById(R.id.logo_toast);
        Glide.with(context).load(R.drawable.logo_high).apply(new RequestOptions().centerCrop()).into(imageView);
        toast.setView(custom);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        return toast;
    }
}
