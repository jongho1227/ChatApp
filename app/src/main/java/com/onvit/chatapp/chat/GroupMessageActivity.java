package com.onvit.chatapp.chat;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.onvit.chatapp.LoginActivity;
import com.onvit.chatapp.PersonInfoActivity;
import com.onvit.chatapp.PreferenceManager;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.NotificationModel;
import com.onvit.chatapp.model.User;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GroupMessageActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXT_STORAGE = 9;
    private final int readMoreChatCount = 100;
    private final int firstReadChatCount = 100;
    int i = 0; // 첫 화면 들어갈때 스크롤 위치 맨 아래로 내리기위함.
    int c = 0;
    Map<String, User> users = new HashMap<>();
    Map<String, Object> messageReadUsers = new HashMap<>();
    Map<String, Object> existUserGroupChat = new HashMap<>();
    List<ChatModel.Comment> comments = new ArrayList<>();
    List<ChatModel.Comment> newComments = new ArrayList<>();
    String toRoom;
    String uid;
    EditText editText;
    int last = 0;
    int commentCount = 0;
    private Toolbar chatToolbar;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat chatDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
    private SimpleDateFormat changeDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 E요일");
    private DatabaseReference databaseReference;

    private ChildEventListener valueEventListener;
    private ValueEventListener accessChatMemberEventListener;
    private ValueEventListener userInfoGetEventListener;
    private RecyclerView recyclerView;
    private GroupMessageRecyclerViewAdapter mFirebaseAdapter;
    private String chatName;
    private TextView sendFile;
    private String checker = "";
    private Uri fileUri;
    private View.OnLayoutChangeListener onLayoutChangeListener;
    private Map<String, Integer> readCount0 = new HashMap<>();
    private FirebaseAuth firebaseAuth;
    private RelativeLayout relativeLayout;

    private String shareText;
    private Uri shareUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_message);
        Log.d("순서", "oncreate");
        firebaseAuth = FirebaseAuth.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        if (firebaseAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            firebaseAuth.signOut();
            startActivity(intent);
            finish();
        }
        long getCount = getIntent().getLongExtra("chatCount",0);
        commentCount = (int) getCount;
        getIntent().addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        initSetting();
        keyboardController();

        init();//메세지 입력했을때 처리 하는부분.

        getMessageList();
        //채팅내용 삭제하는거~
//        removeChat();

    }

    private void initSetting() {
        Log.d("순서", "initSetting");
        relativeLayout = findViewById(R.id.groupMessageActivity_relativelayout);
        sendFile = findViewById(R.id.send_files_btn);
        editText = findViewById(R.id.groupMessageActivity_edittext);
        final TextView pCount = findViewById(R.id.people_count);
        toRoom = getIntent().getStringExtra("toRoom"); // 방이름

//        commentCount = (int) getIntent().getLongExtra("chatCount", 0);
        //알림 관련.
        if (getIntent().getExtras().get("tag") != null) {
            toRoom = (String) getIntent().getExtras().get("tag");
            databaseReference.child("groupChat").child(toRoom).child("comments").orderByChild("readUsers/" + uid).equalTo(false)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot == null) {
                                return;
                            }
                            Map<String, Object> map = new HashMap<>();
                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                                comment.readUsers.put(uid, true);
                                map.put(item.getKey(), comment);
                            }
                            databaseReference.child("groupChat").child(toRoom).child("comments").updateChildren(map);

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }
        //툴바 셋팅
        chatToolbar = findViewById(R.id.chat_toolbar);
        chatToolbar.setBackgroundResource(R.color.chat);
        if (toRoom.equals("normalChat")) {
            chatName = "일반채팅방";
            NotificationManagerCompat.from(this).cancel(toRoom, 0);
            NotificationManagerCompat.from(this).cancel(0);
        } else {
            chatName = "임원채팅방";
            NotificationManagerCompat.from(this).cancel(toRoom, 0);
            NotificationManagerCompat.from(this).cancel(1);
        }
        setSupportActionBar(chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(chatName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        userInfoGetEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("순서", "userInfoGetEventListener");
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    User user = item.getValue(User.class);
                    messageReadUsers.put(user.getUid(), false); // 읽은여부 판단.
                    users.put(item.getKey(), user);//방의 유저정보
                    existUserGroupChat.put(user.getUid(), true);
                    readCount0.put(user.getUid(), 0);
                }
                String number = String.valueOf(users.size());
                pCount.setText(number);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("groupChat").child(toRoom).child("userInfo").addValueEventListener(userInfoGetEventListener);

        accessChatMemberEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    Log.d("순서", "accessChatMemberEventListener");
                    messageReadUsers.put(item.getKey(), item.getValue());

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("groupChat").child(toRoom).child("users").addValueEventListener(accessChatMemberEventListener);


        recyclerView = findViewById(R.id.groupMessageActivity_recyclerView);
        mFirebaseAdapter = new GroupMessageRecyclerViewAdapter(users);
        recyclerView.setLayoutManager(new LinearLayoutManager(GroupMessageActivity.this));
        recyclerView.setAdapter(mFirebaseAdapter);
        sendFile.setOnClickListener(this);
    }

    private void keyboardController() {
        onLayoutChangeListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    v.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            recyclerView.smoothScrollToPosition(mFirebaseAdapter.getItemCount());
                        }
                    }, 0);
                }
            }
        };
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull final RecyclerView recyclerView, int dx, int dy) {
                //키보드 올라오면 채팅창 위로 올라가게.
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                int firstVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
                last = layoutManager.findLastCompletelyVisibleItemPosition();
                if (newComments.size() > firstReadChatCount - 1) {
                    if (firstVisible == 0) {
                        loadChatMore();
                    }
                }

                if (newComments.size() - last < 8) {
                    relativeLayout.setVisibility(View.GONE);
                }

                if (mFirebaseAdapter.getItemCount() == last + 1) {
                    recyclerView.addOnLayoutChangeListener(onLayoutChangeListener);
                } else if (mFirebaseAdapter.getItemCount() - (last + 1) > 9) { // 나중에 고쳐보기
                    recyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
                }

            }
        });
    }

    //채팅내역가지고오는거.
    public void loadChatMore() {
        databaseReference.child("groupChat").child(toRoom).child("comments").orderByChild("timestamp").endAt((Long) newComments.get(0).timestamp)
                .limitToLast(readMoreChatCount).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //채팅없을때 리턴
                if (dataSnapshot.getValue() == null) {
                    return;

                    //맨위로 올라갔을때 리턴.
                } else if (dataSnapshot.getChildrenCount() == 1) {
                    return;

                } else {
                    int l = 0;//채팅 붙이는 인덱스
                    int k = 0;
                    int j = (int) dataSnapshot.getChildrenCount() - 1;//불러오는 채팅갯수
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        if (k == j) {
                            break;
                        }
                        //채팅다붙이면 나감
                        if (l == j) {
                            break;
                        }

                        ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                        //내가없는 채팅은 안가지고옴
                        if (comment.existUser.get(uid) == null) {
                            k++;
                            continue;
                        }
                        newComments.add(l, comment);
                        comments.add(l, comment);
                        k++;
                        l++;
                    }
                    mFirebaseAdapter.notifyDataSetChanged();
                    if (l < readMoreChatCount) {
                        recyclerView.scrollToPosition(last + l);
                    } else {
                        recyclerView.scrollToPosition(last + readMoreChatCount);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    void init() {
        Log.d("순서", "init");
        Button button = findViewById(R.id.groupMessageActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String text = editText.getText().toString();
                editText.setText("");
                sendMessage(text);
            }
        });
    }

    private void sendMessage(final String text) {
        final Date date = new Date();
        final List<String> registration_ids = new ArrayList<>();
        final ChatModel.Comment comment = new ChatModel.Comment();
        //채팅 내용이 있으면.
        if (text != null && !text.replace(" ", "").equals("")) {
            messageReadUsers.put(uid, true);//메세지 작성시 읽음으로 표시 = true
            comment.uid = uid; // 채팅친사람
            comment.message = text; // 채팅친내용
            comment.timestamp = date.getTime(); // 채팅친 시간
            comment.type = "text"; // 채팅 친 종류
            comment.readUsers = messageReadUsers; // 읽은사람들
            comment.existUser = existUserGroupChat;

            databaseReference.child("groupChat").child(toRoom).child("comments").push().setValue(comment);

            databaseReference.child("groupChat").child(toRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                        if (item.getValue().toString().equals("false") || (Boolean) item.getValue() == false) {
                            //fcm보내기
                            if (users.get(item.getKey()).getPushToken() == null || users.get(item.getKey()).getPushToken().equals("null") || users.get(item.getKey()).getPushToken().equals("")) { // 토큰값 없는애들도 제외
                                continue;
                            }
                            registration_ids.add(users.get(item.getKey()).getPushToken());


                        }
                    }
                    sendFcm(registration_ids, comment.message);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            // 마지막 채팅 내용 표시하기.
            //각 사람별 안읽은 메세지 숫자를 가지고 옴.
            databaseReference.child("lastChat").child(toRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Map<String, Object> unreadUser = (Map<String, Object>) dataSnapshot.getValue();
                    Map<String, Object> read = comment.readUsers; // 읽은사람구분
                    Set<String> keys = read.keySet();
                    Iterator<String> it = keys.iterator();
                    while (it.hasNext()) {
                        String key1 = it.next();
                        Object value = read.get(key1);
                        if ((Boolean) value == false) {
                            //메세지 안읽었으면 기존꺼에서 1추가함.
                            unreadUser.put(key1, ((int) (long) unreadUser.get(key1)) + 1);
                        }
                    }
                    Map<String, Object> lastMap = new HashMap<>();
                    lastMap.put("lastChat", comment.message);
                    lastMap.put("timestamp", comment.timestamp);
                    lastMap.put("users", unreadUser);
                    FirebaseDatabase.getInstance().getReference().child("lastChat").child(toRoom).updateChildren(lastMap); // 마지막 메세지 표시
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        } else {//아무것도 안치거나 공백이면 그냥 리턴.
            return;
        }
    }

    private void sendImg(Uri fileUri) {
        Bitmap bitmap = resize(this, fileUri, 500);
        String filePath = getRealPathFromURI(fileUri);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int orientation  = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Bitmap newBitmap = rotateBitmap(bitmap, orientation);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();
        final DatabaseReference userMessageKeyRef = databaseReference.child("groupChat").child(toRoom).child("comments").push();
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files").child(userMessageKeyRef.getKey());
        UploadTask uploadTask = storageReference.putBytes(bytes);

        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return storageReference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            //storageReference 에 저장한 이미지 uri를 불러옴
            @Override
            public void onComplete(@NonNull final Task<Uri> task) {
                if (task.isSuccessful()) {
                    //이미지 저장 성공
                    final Date date = new Date();
                    final List<String> registration_ids = new ArrayList<>();
                    Uri taskResult = task.getResult();
                    String imageUri = taskResult.toString();
                    final ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = imageUri;
                    comment.timestamp = date.getTime();
                    comment.readUsers = messageReadUsers;
                    comment.type = "img";
                    comment.existUser = existUserGroupChat;
                    databaseReference.child("groupChat").child(toRoom).child("comments").child(userMessageKeyRef.getKey()).setValue(comment).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            databaseReference.child("lastChat").child(toRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Map<String, Object> unreadUser = (Map<String, Object>) dataSnapshot.getValue();
                                    Map<String, Object> read = comment.readUsers;
                                    Set<String> keys = read.keySet();
                                    Iterator<String> it = keys.iterator();
                                    while (it.hasNext()) {
                                        String key1 = it.next();
                                        Object value = read.get(key1);
                                        if ((Boolean) value == false) {
                                            //메세지 안읽었으면 기존꺼에서 1추가함.
                                            unreadUser.put(key1, ((int) (long) unreadUser.get(key1)) + 1);
                                        }
                                    }
                                    Map<String, Object> lastMap = new HashMap<>();
                                    lastMap.put("lastChat", "사진을 보냈습니다.");
                                    lastMap.put("timestamp", comment.timestamp);
                                    lastMap.put("users", unreadUser);
                                    FirebaseDatabase.getInstance().getReference().child("lastChat").child(toRoom).updateChildren(lastMap); // 마지막 메세지 표시
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                            databaseReference.child("groupChat").child(toRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                                        if (item.getValue().toString().equals("false") || (Boolean) item.getValue() == false) {
                                            //fcm보내기
                                            if (users.get(item.getKey()).getPushToken() == null || users.get(item.getKey()).getPushToken().equals("null") || users.get(item.getKey()).getPushToken().equals("")) { // 토큰값 없는애들도 제외
                                                continue;
                                            }
                                            registration_ids.add(users.get(item.getKey()).getPushToken());
                                        }
                                    }
                                    sendFcm(registration_ids, "사진을 보냈습니다.");
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                }
                            });

                        }
                    });
                }
            }
        });
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    //사진 사이즈(용량)조절
    private Bitmap resize(Context context, Uri uri, int resize) {
        Bitmap resizeBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options); // 1번

            int width = options.outWidth;
            int height = options.outHeight;

            int samplesize = 1;

            while (true) {//2번
                if (width / 2 < resize || height / 2 < resize) {
                    break;
                }

                width /= 2;
                height /= 2;
                samplesize *= 2;
            }


            options.inSampleSize = samplesize;
            Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options); //3번
            resizeBitmap = bitmap;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return resizeBitmap;
    }

    //fcm보내는 부분
    void sendFcm(List<String> registration_ids, String message) {
        Gson gson = new Gson();

        String userName = PreferenceManager.getString(GroupMessageActivity.this, "name");
        NotificationModel notificationModel = new NotificationModel();
        notificationModel.registration_ids = registration_ids;
        notificationModel.notification.title = userName;
        notificationModel.notification.text = message;
        notificationModel.notification.tag = toRoom;
        notificationModel.notification.click_action = "GroupMessage";

        notificationModel.data.title = userName;
        notificationModel.data.text = message;
        notificationModel.data.tag = toRoom;
        notificationModel.data.click_action = "GroupMessage";

        RequestBody requestBody = RequestBody.create(gson.toJson(notificationModel), MediaType.parse("application/json; charset=utf8"));
        Request request = new Request.Builder().header("Content-Type", "apllication/json")
                .addHeader("Authorization", "key=AAAAjkt-NJ4:APA91bF8vZrFrqLIRfpPwE_WvUrGj4aQEP8xF9_UvvG4MZXA2iV-o7NPAJdGGYhlMl_JXP8KQiF_YWQeVhT0DE8BSppJUfYazA0QR7tjozAdpzMvX9xLSHJ1mkOevT4_OlohvlOYS_e-")
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.send_files_btn:
                CharSequence options[] = new CharSequence[]{
                        "사진",
                        "파일",
//                        "워드파일"
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(GroupMessageActivity.this);
                builder.setTitle("파일 선택");

                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            checker = "image";
                            Intent intent = new Intent(Intent.ACTION_PICK);
                            intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                            getIntent().putExtra("on", "on");
                            startActivityForResult(intent.createChooser(intent, "사진 선택"), 438);
                        }
                        if (which == 1) {
                            checker = "PDF";
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                            intent.setType("application/*");
                            getIntent().putExtra("on", "on");
                            startActivityForResult(intent.createChooser(intent, "파일 선택"), 438);
                        }
//                        if (which == 2) {
//                            checker = "docx";
//                            Intent intent = new Intent();
//                            intent.setAction(Intent.ACTION_GET_CONTENT);
//                            intent.setType("application/msword");
//                            startActivityForResult(intent.createChooser(intent, "MS Word파일 선택"), 438);
//                        }
                    }
                });
                builder.show();

                break;
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            fileUri = data.getData();
            final String fileName = getFileName(fileUri);
            if (!checker.equals("image")) {
                try {
                    Log.d("확장자", fileUri.toString());
                    getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    String extension = fileName.substring(fileName.lastIndexOf(".")+1);
                    final DatabaseReference userMessageKeyRef = databaseReference.child("groupChat").child(toRoom).child("comments").push();
                    final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files").child(userMessageKeyRef.getKey()+"."+extension);
                    UploadTask uploadTask = storageReference.putFile(fileUri);
                    uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            return storageReference.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                //이미지 저장 성공
                                final Date date = new Date();
                                final List<String> registration_ids = new ArrayList<>();
                                Uri taskResult = task.getResult();
                                String fileUri = taskResult.toString();
                                final ChatModel.Comment comment = new ChatModel.Comment();
                                comment.uid = uid;
                                comment.message = "파일을 보냈습니다\n" + fileName + fileUri;
                                comment.timestamp = date.getTime();
                                comment.readUsers = messageReadUsers;
                                comment.type = "file";
                                comment.existUser = existUserGroupChat;
                                databaseReference.child("groupChat").child(toRoom).child("comments").child(userMessageKeyRef.getKey()).setValue(comment).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        databaseReference.child("lastChat").child(toRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                Map<String, Object> unreadUser = (Map<String, Object>) dataSnapshot.getValue();
                                                Map<String, Object> read = comment.readUsers;
                                                Set<String> keys = read.keySet();
                                                Iterator<String> it = keys.iterator();
                                                while (it.hasNext()) {
                                                    String key1 = it.next();
                                                    Object value = read.get(key1);
                                                    if ((Boolean) value == false) {
                                                        //메세지 안읽었으면 기존꺼에서 1추가함.
                                                        unreadUser.put(key1, ((int) (long) unreadUser.get(key1)) + 1);
                                                    }
                                                }
                                                Map<String, Object> lastMap = new HashMap<>();
                                                lastMap.put("lastChat", "파일을 보냈습니다.");
                                                lastMap.put("timestamp", comment.timestamp);
                                                lastMap.put("users", unreadUser);
                                                FirebaseDatabase.getInstance().getReference().child("lastChat").child(toRoom).updateChildren(lastMap); // 마지막 메세지 표시
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });

                                        databaseReference.child("groupChat").child(toRoom).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                                                for (DataSnapshot item : dataSnapshot.getChildren()) {
                                                    if (item.getValue().toString().equals("false") || (Boolean) item.getValue() == false) {
                                                        //fcm보내기
                                                        if (users.get(item.getKey()).getPushToken() == null || users.get(item.getKey()).getPushToken().equals("null") || users.get(item.getKey()).getPushToken().equals("")) { // 토큰값 없는애들도 제외
                                                            continue;
                                                        }
                                                        registration_ids.add(users.get(item.getKey()).getPushToken());
                                                    }
                                                }
                                                sendFcm(registration_ids, "파일을 보냈습니다.");
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                            }
                                        });

                                    }
                                });
                            }
                        }
                    });
                } catch (Exception e) {
                    Toast.makeText(GroupMessageActivity.this, "보낼 수 없는 파일 형식입니다.", Toast.LENGTH_SHORT).show();
                }

            } else if (checker.equals("image")) {
                sendImg(fileUri);
            } else {
                Toast.makeText(this, "Error : Nothing Selected", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private String getRealPathFromURI(Uri fileUri) {
        String result;
        Cursor cursor = getContentResolver().query(fileUri, null, null,null,null);
        if(cursor==null){
            result = fileUri.getPath();
        }else{
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


    //메세지 화면에 표시하는 부분.
    void getMessageList() {
        Log.d("순서", "getMessageList");
        valueEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                //채팅내역 없으면 리턴
                //채팅을 치면 자기 자신은 읽음으로 처리해서 넘어간다.
                //채팅이 업데이트 되면 여기로 들어오니까, 넘어온 채팅에서 접속해있는 클라이언트도 읽음으로 처리해서 업데이트 해야된다.

                //누군가가 채팅방으로 들어오면 그사람이 안읽은 메세지들이 업데이트 되는데 그건 onChildChanged에서 처리해야 된다.
                c++;
                ChatModel.Comment comment_modify = dataSnapshot.getValue(ChatModel.Comment.class);

                //화면에 뿌리는 코멘트.

                newComments.add(comment_modify);

                //commentCount = 처음 방에 진입했을 시점에 존재하는 총 코멘트 수
                //firstReadChatCount = 처음 불러오는 코멘트 개수
                //총 코멘트의 수가 처음 불러오는 코멘트 개수보다 적으면
                if (commentCount < firstReadChatCount) {
                    if (commentCount == c || commentCount+1 == c ) {
                        shareProcess();
                    }
                    //c = 추가되는 코멘트의 수, 실시간 반영되는 총 코멘트 숫자임.
                    //ex) firstReadChatCount = 10개, commentCount= 5개라고 했을 시
                    //c가 commentCount랑 같아질때까진 담기만함.
                    if (c < commentCount) {

                        return;
                    } else {
                        //c가 commentCount보다 같거나 커지면 화면에 그리기 시작.

                        readMessage();
                    }
                } else {
                    if (c < firstReadChatCount) {
                        return;
                    } else {
                        readMessage();
                    }
                    if (firstReadChatCount == c) {
                        shareProcess();
                    }


                }
            }

            private void readMessage() {
                if (!newComments.get(newComments.size() - 1).uid.equals(uid)) {// 채팅방에 안읽은 메세지가 있을때 들어오면 or  상대방이 채팅쳤을때
                    if (i == 0) {
                        mFirebaseAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - 1);
                        i++;
                    } else {
                        if (newComments.size() - last > 8) {
                            //스크롤하면 없어짐. 왜?
                            ImageView imageView = findViewById(R.id.rel_img);
                            TextView nameText = findViewById(R.id.text_name);
                            TextView messageText = findViewById(R.id.text_message);
                            if (users.get(newComments.get(newComments.size() - 1).uid).getUserProfileImageUrl().equals("noImg")) {
                                Glide.with(GroupMessageActivity.this).load(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(imageView);
                            } else {
                                Glide.with(GroupMessageActivity.this).load(users.get(newComments.get(newComments.size() - 1).uid).getUserProfileImageUrl()).apply(new RequestOptions().centerCrop()).into(imageView);
                            }

                            GradientDrawable gradientDrawable = (GradientDrawable) GroupMessageActivity.this.getDrawable(R.drawable.radius);
                            imageView.setBackground(gradientDrawable);
                            imageView.setClipToOutline(true);
                            nameText.setText(users.get(newComments.get(newComments.size() - 1).uid).getUserName());
                            if (newComments.get(newComments.size() - 1).type.equals("img")) {
                                messageText.setText("사진을 보냈습니다.");
                            } else {
                                messageText.setText(newComments.get(newComments.size() - 1).message);
                            }

                            relativeLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    recyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - 1);
                                }
                            });
                            relativeLayout.setVisibility(View.VISIBLE);

                        }
                        mFirebaseAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(last + 1);
                    }

                } else {
                    mFirebaseAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - 1);
                    i++;
                }
                // 메시지 불러오고 안읽은 메세지 있으면 모두 읽음 표시로 바꾸고 안읽은 메세지 개수 0으로 만듬.
                Map<String, Object> map = new HashMap<>();
                map.put(uid, 0);
                databaseReference.child("lastChat").child(toRoom).child("users").updateChildren(map);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                final ChatModel.Comment comment = dataSnapshot.getValue(ChatModel.Comment.class);
                final int a = newComments.indexOf(comment);
                newComments.remove(a);

                newComments.add(a, comment);

                mFirebaseAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("groupChat").child(toRoom).child("comments").orderByChild("existUser/" + uid).equalTo(true).limitToLast(firstReadChatCount).addChildEventListener(valueEventListener);
    }


    @Override
    protected void onResume() {
        super.onResume();
        getIntent().removeExtra("on");
        Map<String, Object> map = new HashMap<>();
        map.put(uid, true);
        databaseReference.child("groupChat").child(toRoom).child("users").updateChildren(map);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (getIntent().getStringExtra("on") == null) {
            Map<String, Object> map = new HashMap<>();
            map.put(uid, false);
            databaseReference.child("groupChat").child(toRoom).child("users").updateChildren(map);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReference.child("groupChat").child(toRoom).child("comments").removeEventListener(valueEventListener); // 이벤트 제거.
        }
        if (userInfoGetEventListener != null) {
            databaseReference.child("groupChat").child(toRoom).child("userInfo").removeEventListener(userInfoGetEventListener);
        }
        if (accessChatMemberEventListener != null) {
            databaseReference.child("groupChat").child(toRoom).child("users").removeEventListener(accessChatMemberEventListener);
        }
        Map<String, Object> map = new HashMap<>();
        map.put(uid, false);
        databaseReference.child("groupChat").child(toRoom).child("users").updateChildren(map);
    }

    //뒤로가기 눌렀을때
    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromleft, R.anim.toright);//화면 사라지는 방향
    }

    //툴바에 뒤로가기 버튼
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void shareProcess() {
        if (getIntent().getStringExtra("shareText") != null) {
            shareText = getIntent().getStringExtra("shareText");
            getIntent().removeExtra("shareText");
            sendMessage(shareText);
        }
        if (getIntent().getParcelableExtra("shareUri") != null) {
            shareUri = getIntent().getParcelableExtra("shareUri");
            getIntent().removeExtra("shareUri");
            sendImg(shareUri);
        }
    }

    class GroupMessageRecyclerViewAdapter extends RecyclerView.Adapter<GroupMessageRecyclerViewAdapter.groupViewHolder> {

        Map<String, User> users;

        public GroupMessageRecyclerViewAdapter(Map<String, User> users) {
            this.users = users;
            Log.d("순서", "GroupMessageRecyclerViewAdapter");
        }

        @NonNull
        @Override
        public GroupMessageRecyclerViewAdapter.groupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            Log.d("순서", "onCreateViewHolder");
            return new groupViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull GroupMessageRecyclerViewAdapter.groupViewHolder holder, int position) {
            //position이 큰거부터 붙음. 거꾸로감.
            Log.d("순서", "onBindViewHolder");
            holder.linearLayout_change_date.setVisibility(View.GONE);

            //채팅 안읽은 숫자 표시
            holder.textView_readCounter_left.setVisibility(View.INVISIBLE);
            holder.textView_readCounter_right.setVisibility(View.INVISIBLE);

            //상대 이름표시
            holder.textView_name.setVisibility(View.VISIBLE);

            holder.linearLayout_my.setVisibility(View.GONE);
            holder.linearLayout_other.setVisibility(View.GONE);

            //이미지보낼때 쓰는 뷰
            holder.imageView.setVisibility(View.GONE);
            //텍스트보낼때 쓰는 뷰
            holder.textView_message.setVisibility(View.GONE);

            //메세지 보낸 시간.
            long unixTime = (long) newComments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            changeDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            if (position > 0) {
                long preTime = (long) newComments.get(position - 1).timestamp;
                long nowTime = (long) newComments.get(position).timestamp;
                Date preDateTime = new Date(preTime);
                Date nowDateTime = new Date(nowTime);
                String preChatTime = changeDateFormat.format(preDateTime);
                String nowChatTime = changeDateFormat.format(nowDateTime);
                if (preChatTime.equals(nowChatTime)) {
                    holder.linearLayout_change_date.setVisibility(View.GONE);
                } else {
                    holder.linearLayout_change_date.setVisibility(View.VISIBLE);
                    holder.messageItem_change_date_textview.setText(nowChatTime);
                }
            } else {
                long nowTime = (long) newComments.get(position).timestamp;
                Date nowDateTime = new Date(nowTime);
                String nowChatTime = changeDateFormat.format(nowDateTime);
                holder.linearLayout_change_date.setVisibility(View.VISIBLE);
                holder.messageItem_change_date_textview.setText(nowChatTime);
            }

            //내가보낸메세지
            if (newComments.get(position).uid.equals(uid)) {
                //이름 없앰
                holder.textView_name.setVisibility(View.GONE);

                //텍스트를 보냈을때
                if (newComments.get(position).type.equals("text")) {

                    holder.textView_message.setVisibility(View.VISIBLE);
                    holder.textView_message.setText(newComments.get(position).message);
                    holder.textView_message.setBackgroundResource(R.drawable.sender_message_layout);

                    //복사 및 공유 기능
                    longClick(holder);

                    //이미지를 보냈을때
                } else if (newComments.get(position).type.equals("img")) {
                    holder.imageView.setVisibility(View.VISIBLE);
                    //이미지 처리하는 부분
                    Glide.with(holder.itemView.getContext()).load(newComments.get(position).message).apply(new RequestOptions().fitCenter()).into(holder.imageView);
                    GradientDrawable gradientDrawable = (GradientDrawable) GroupMessageActivity.this.getDrawable(R.drawable.radius);
                    holder.imageView.setBackground(gradientDrawable);
                    holder.imageView.setClipToOutline(true);

                    imgDownLoad(holder, newComments.get(position).message, position);
                    //공유 기능.
                    imgLongClick(holder, newComments.get(position).message);

                } else if (newComments.get(position).type.equals("file")) {
                    String me = "me";
                    getFile(holder, position, me);

                    //복사 및 공유 기능
//                    longClick(holder);
                }

                holder.linearLayout_to.setVisibility(View.INVISIBLE);
                //메세지 레이아웃 오른쪽으로
                holder.linearlayout_main.setGravity(Gravity.RIGHT);

                //안읽은 사람 몇명인지 처리

                setReadCounter(position, holder.textView_readCounter_left);

                holder.linearLayout_my.setVisibility(View.VISIBLE);
                holder.textView_mytimestamp.setText(time);

                //상대방이보낸메세지
            } else {

                if (position > 0) {
                    long preTime = (long) newComments.get(position - 1).timestamp;
                    long nowTime = (long) newComments.get(position).timestamp;
                    Date preDateTime = new Date(preTime);
                    Date nowDateTime = new Date(nowTime);
                    chatDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                    String preChatTime = chatDateFormat.format(preDateTime);
                    String nowChatTime = chatDateFormat.format(nowDateTime);
                    //전에 채팅이랑 동일한 사람인데
                    if (newComments.get(position - 1).uid.equals(newComments.get(position).uid)) {
                        //보낸시간이 같으면~
                        if (nowChatTime.equals(preChatTime)) {
                            holder.linearLayout_to.setVisibility(View.INVISIBLE);
                            holder.textView_name.setVisibility(View.GONE);
                        } else {//보낸시간은 다름
                            yourProfileImage(holder, position);
                        }
                    } else {//다른사람
                        yourProfileImage(holder, position);
                    }
                } else {
                    //상대방 사진 표시
                    yourProfileImage(holder, position);
                }
                //텍스트 보냈을때
                if (newComments.get(position).type.equals("text")) {
                    holder.textView_message.setVisibility(View.VISIBLE);
                    holder.textView_message.setBackgroundResource(R.drawable.receiver_messages_layout);
                    holder.textView_message.setText(newComments.get(position).message);
                    longClick(holder);
                    //이미지 보냈을때
                } else if (newComments.get(position).type.equals("img")) {

                    holder.imageView.setVisibility(View.VISIBLE);
                    Glide.with(holder.itemView.getContext()).load(newComments.get(position).message).apply(new RequestOptions().fitCenter()).into(holder.imageView);
                    GradientDrawable gradientDrawable = (GradientDrawable) GroupMessageActivity.this.getDrawable(R.drawable.radius);
                    holder.imageView.setBackground(gradientDrawable);
                    holder.imageView.setClipToOutline(true);
                    imgDownLoad(holder, newComments.get(position).message, position);
                    imgLongClick(holder, newComments.get(position).message);
                } else if (newComments.get(position).type.equals("file")) {
                    String other = "other";
                    getFile(holder, position,other);
//                    longClick(holder);
                }
                holder.linearlayout_main.setGravity(Gravity.LEFT);
                setReadCounter(position, holder.textView_readCounter_right);
                holder.linearLayout_other.setVisibility(View.VISIBLE);
                holder.textView_othertimestamp.setText(time);
            }
        }

        private void getFile(@NonNull groupViewHolder holder, int position, String identify) {
            holder.textView_message.setVisibility(View.VISIBLE);
            final String message = newComments.get(position).message;
            final int lastIndex = message.lastIndexOf("https://");
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View view) {//
                    try{
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        if(message.contains(".pdf")){
                            intent.setDataAndType(Uri.parse(message.substring(lastIndex)), "application/pdf");
                        }else{
                            intent.setDataAndType(Uri.parse(message.substring(lastIndex)), "application/*");
                        }
                        startActivity(intent);
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(GroupMessageActivity.this, "파일을 열 수 없습니다. 뷰어을 설치해 주세요.", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            SpannableString spannableString = new SpannableString(message.substring(0, lastIndex));
            spannableString.setSpan(clickableSpan, 9, lastIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new UnderlineSpan(), 9, lastIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                    spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#FF00DD")), 9, lastIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.textView_message.setText(spannableString);

            holder.textView_message.setClickable(true);
            holder.textView_message.setMovementMethod(LinkMovementMethod.getInstance());
            if(identify.equals("me")){
                holder.textView_message.setBackgroundResource(R.drawable.sender_message_layout);
            }else{
                holder.textView_message.setBackgroundResource(R.drawable.receiver_messages_layout);
            }

        }

        private void imgDownLoad(final groupViewHolder holder, final String message, final int position) {
            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(GroupMessageActivity.this, BigPictureActivity.class);
                    intent.putExtra("uri", message);
                    SimpleDateFormat sd = new SimpleDateFormat("yyyy/MM/dd");
                    String code = newComments.get(position).uid;
                    User imgUser = users.get(code);
                    String name = imgUser.getUserName();
                    intent.putExtra("name", name);
                    getIntent().putExtra("on", "on");
                    startActivity(intent);
                }
            });
        }

        private void yourProfileImage(@NonNull groupViewHolder holder, final int position) {
            //상대방 사진 표시
            if ((users.get(newComments.get(position).uid).getUserProfileImageUrl()).equals("noImg")) {
                Glide.with(holder.itemView.getContext()).load(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(holder.imageView_profile);
            } else {
                Glide.with(holder.itemView.getContext()).load(users.get(newComments.get(position).uid).getUserProfileImageUrl()).apply(new RequestOptions().centerCrop()).into(holder.imageView_profile);
            }
            GradientDrawable profile = (GradientDrawable) GroupMessageActivity.this.getDrawable(R.drawable.radius);
            holder.imageView_profile.setBackground(profile);
            holder.imageView_profile.setClipToOutline(true);
            //상대방 이름 표시
            holder.textView_name.setText(users.get(newComments.get(position).uid).getUserName() + "(" + users.get(newComments.get(position).uid).getHospital() + ")");
            holder.linearLayout_to.setVisibility(View.VISIBLE);
            holder.imageView_profile.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(GroupMessageActivity.this, PersonInfoActivity.class);
                    getIntent().putExtra("on", "on");
                    intent.putExtra("info", users.get(newComments.get(position).uid).getUid());

                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            });
        }

        private void imgLongClick(final groupViewHolder holder, final String path) {
            holder.imageView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    CharSequence options[] = new CharSequence[]{
                            "공유"
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupMessageActivity.this);
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
//                                Intent intent = new Intent(GroupMessageActivity.this, BigPictureActivity.class);
                                Bitmap bitmap = ((BitmapDrawable) holder.imageView.getDrawable()).getBitmap();
                                String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(System.currentTimeMillis());
                                String imagename = time + ".PNG";
                                File path = getFilesDir();
                                File file = new File(path, imagename);
                                OutputStream out;
                                try {
                                    out = new FileOutputStream(file);
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                                    out.flush();
                                    out.close();
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    Uri uri = null;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {// API 24 이상 일경우..
                                        uri = FileProvider.getUriForFile(GroupMessageActivity.this, getApplicationContext().getPackageName() + ".fileprovider", file);
                                    } else {// API 24 미만 일경우..
                                        uri = Uri.fromFile(file);
                                    }
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                    shareIntent.setType("image/*");
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, "친구에게 공유하기"));

                                } catch (Exception e) {
                                    Toast.makeText(GroupMessageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                }

                            }
                        }
                    });
                    builder.show();
                    return true;
                }
            });
        }

        private void longClick(@NonNull groupViewHolder holder) {
            holder.textView_message.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    TextView tv = (TextView) view;
                    final String m = tv.getText().toString();
                    CharSequence options[] = new CharSequence[]{
                            "복사",
                            "공유",
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(GroupMessageActivity.this);
                    builder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                ClipboardManager clip = (ClipboardManager) GroupMessageActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData clipData = ClipData.newPlainText("복사", m);
                                clip.setPrimaryClip(clipData);
                            }
                            if (which == 1) {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                intent.setType("text/plain");
                                // Set default text message
                                // 카톡, 이메일, MMS 다 이걸로 설정 가능
                                //String subject = "문자의 제목";
                                String text = m;
                                //intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                                intent.putExtra(Intent.EXTRA_TEXT, text);

                                // Title of intent
                                Intent chooser = Intent.createChooser(intent, "친구에게 공유하기");
                                startActivity(chooser);
                                finish();
                            }
                        }
                    });
                    builder.show();
                    return true;
                }
            });
        }

        private Uri getConvertUri(Uri uri) {
            try {
                File file = new File(Environment.getExternalStorageDirectory() + "/KCHA", System.currentTimeMillis() + ".jpeg");
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    OutputStream outputStream = new FileOutputStream(file);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                    outputStream.close();
                    inputStream.close();
                }
                if (file.length() > 0) {
                    return new Uri.Builder().scheme("file").path(file.getAbsolutePath()).build();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public int getItemCount() {
            return newComments.size();
        }


        void setReadCounter(final int position, final TextView textView) { // 읽음 표시하는부분
            int userNumber = 0;
            textView.setVisibility(View.INVISIBLE);
            Map<String, Object> read = newComments.get(position).readUsers;
            Set<String> keys = read.keySet();
            Iterator<String> it = keys.iterator();
            while (it.hasNext()) {
                String key = it.next();
                Object value = read.get(key);
                if ((Boolean) value == false) {
                    userNumber += 1;
                }
            }
            if (userNumber > 0) {
                textView.setText(String.valueOf(userNumber));
                textView.setVisibility(View.VISIBLE);
            } else {
                textView.setVisibility(View.INVISIBLE);
            }
        }


        private class groupViewHolder extends RecyclerView.ViewHolder {
            private TextView textView_message; //메세지
            private TextView textView_name; // 이름
            private ImageView imageView_profile; // 프로필
            private LinearLayout linearLayout_to; // 프로필이미지뷰 담고있는 레이아웃
            private LinearLayout linearlayout_main;//전체 레이아웃
            private TextView textView_mytimestamp; // 시간
            private TextView textView_othertimestamp;
            private TextView textView_readCounter_left; // 읽은 숫자
            private TextView textView_readCounter_right;
            private LinearLayout linearLayout_my;
            private LinearLayout linearLayout_other;
            private ImageView imageView; // 이미지
            private LinearLayout linearLayout_change_date;
            private TextView messageItem_change_date_textview;

            private groupViewHolder(@NonNull View view) {
                super(view);
                textView_message = view.findViewById(R.id.messageItem_textView_message);
                textView_name = view.findViewById(R.id.messageItem_textview_name);
                imageView_profile = view.findViewById(R.id.messageItem_imageview_profile);

                linearLayout_to = view.findViewById(R.id.messageItem_linearlayout_to);

                linearlayout_main = view.findViewById(R.id.messageItem_linearlayout_main);

                linearLayout_change_date = view.findViewById(R.id.messageItem_linearlayout_change_date);

                textView_mytimestamp = view.findViewById(R.id.messageItem_textView_mytimestamp);
                textView_othertimestamp = view.findViewById(R.id.messageItem_textView_othertimestamp);
                textView_readCounter_left = view.findViewById(R.id.messageItem_textview_readCounter_left);
                textView_readCounter_right = view.findViewById(R.id.messageItem_textview_readCounter_right);
                linearLayout_my = view.findViewById(R.id.messageItem_Linearlayout_mytime);
                linearLayout_other = view.findViewById(R.id.messageItem_Linearlayout_othertime);
                imageView = view.findViewById(R.id.messageItem_image);

                messageItem_change_date_textview = view.findViewById(R.id.messageItem_change_date_textview);

            }
        }
    }
}
