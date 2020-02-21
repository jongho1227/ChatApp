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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.onvit.chatapp.BuildConfig;
import com.onvit.chatapp.LoginActivity;
import com.onvit.chatapp.R;
import com.onvit.chatapp.chat.vote.VoteActivity;
import com.onvit.chatapp.contact.PersonInfoActivity;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.model.Img;
import com.onvit.chatapp.model.NotificationModel;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.util.PreferenceManager;
import com.onvit.chatapp.util.Utiles;
import com.vlk.multimager.activities.GalleryActivity;
import com.vlk.multimager.utils.Constants;
import com.vlk.multimager.utils.Image;
import com.vlk.multimager.utils.Params;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
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

    private final int readMoreChatCount = 300;
    private final int firstReadChatCount = 500;
    int i = 0; // 첫 화면 들어갈때 스크롤 위치 맨 아래로 내리기위함.
    int c = 0;
    Map<String, User> users = new HashMap<>();
    Map<String, Object> messageReadUsers = new HashMap<>();
    Map<String, Object> existUserGroupChat = new HashMap<>();
    List<ChatModel.Comment> comments = new ArrayList<>();
    List<ChatModel.Comment> newComments = new ArrayList<>();
    List<User> userinfolist = new ArrayList<>();
    InputStream inputStream;

    String toRoom;
    String uid;
    EditText editText;
    int last = 0;
    int commentCount = 0;
    String uriText;
    int pWidth, pHeight;
    private Toolbar chatToolbar;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
    private SimpleDateFormat chatDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
    private SimpleDateFormat changeDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일 E요일", Locale.KOREA);
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
    private FirebaseAuth firebaseAuth;
    private RelativeLayout relativeLayout;
    private String shareText;
    private Uri shareUri;
    private List<Img> img_list = new ArrayList<>();
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
        long getCount = getIntent().getLongExtra("chatCount", 0);
        toRoom = getIntent().getStringExtra("toRoom"); // 방이름
        commentCount = (int) getCount;

        getIntent().addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        getIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);


        initSetting();
    }


    private void initSetting() {
        Log.d("순서", "initSetting");
        relativeLayout = findViewById(R.id.groupMessageActivity_relativelayout);
        sendFile = findViewById(R.id.send_files_btn);
        editText = findViewById(R.id.groupMessageActivity_edittext);
        final TextView pCount = findViewById(R.id.people_count);

        //툴바 셋팅
        chatToolbar = findViewById(R.id.chat_toolbar);
        chatToolbar.setBackgroundResource(R.color.chat);


        userInfoGetEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("순서", "userInfoGetEventListener");
                User myInfo = new User();
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    User user = item.getValue(User.class);
//                    messageReadUsers.put(user.getUid(), false); // 읽은여부 판단.
                    users.put(item.getKey(), user);//방의 유저정보
                    if (user.getUid().equals(uid)) {
                        myInfo = user;
                        continue;
                    } else {
                        userinfolist.add(user);
                    }
                }
                Collections.sort(userinfolist);
                userinfolist.add(0, myInfo);
                String number = String.valueOf(users.size());
                pCount.setText("채팅인원 : " + number + "명");
                pCount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(GroupMessageActivity.this, ChatSetInfoActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        intent.putParcelableArrayListExtra("userInfo", (ArrayList<? extends Parcelable>) userinfolist);
                        intent.putParcelableArrayListExtra("imglist", (ArrayList<? extends Parcelable>) img_list);
                        intent.putExtra("room", toRoom);
                        getIntent().putExtra("on", "on");
                        overridePendingTransition(R.anim.fromright, R.anim.toleft);
                        startActivity(intent);
                    }
                });

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
                    existUserGroupChat.put(item.getKey(), true);
                }
                if (commentCount == 0) {
                    if (getIntent().getStringExtra("shareText") != null) {
                        shareText = getIntent().getStringExtra("shareText");
                        getIntent().removeExtra("shareText");
                        sendMessage(shareText);
                    }
                    if (getIntent().getParcelableExtra("shareUri") != null) {
                        shareUri = getIntent().getParcelableExtra("shareUri");
                        getIntent().removeExtra("shareUri");
                        if (createImgSize(shareUri)) return;
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        databaseReference.child("groupChat").child(toRoom).child("users").addValueEventListener(accessChatMemberEventListener);


        recyclerView = findViewById(R.id.groupMessageActivity_recyclerView);
        recyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mFirebaseAdapter = new GroupMessageRecyclerViewAdapter(users);
                recyclerView.setLayoutManager(new LinearLayoutManager(GroupMessageActivity.this));
                recyclerView.setAdapter(mFirebaseAdapter);
                init();//메세지 입력했을때 처리 하는부분.
                getMessageList();
                keyboardController();
            }
        }, 200);
        sendFile.setOnClickListener(this);
    }

    private void keyboardController() {
        onLayoutChangeListener = new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Log.d("바텀", "바텀" + bottom);
                Log.d("바텀", "올드바텀" + oldBottom);
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
                last = layoutManager.findLastVisibleItemPosition(); // 화면 맨 아래 채팅 인덱스

                if (newComments.size() > firstReadChatCount - 1) {
                    if (firstVisible == 0) {
                        loadChatMore();
                    }
                }
                if (newComments.size() - last < 3) {
                    relativeLayout.setVisibility(View.GONE);
                }

                if (mFirebaseAdapter.getItemCount() == last + 1 && newComments.get(last).message.length() < 550) {
                    Log.d("들오오", "ㅇㅇ");
                    recyclerView.addOnLayoutChangeListener(onLayoutChangeListener);
                } else if (mFirebaseAdapter.getItemCount() - last > 8) { // 나중에 고쳐보기
                    Log.d("들오오", "ㄴㄴ");
                    recyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
                } else if (newComments.get(last).message.length() > 550) {
                    Log.d("들오오", "ㄴㄴ22");
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
                Log.d("불러오는채팅갯수", dataSnapshot.getChildrenCount() + "");
                Log.d("불러오는채팅갯수", newComments.get(0).timestamp + "");
                //채팅없을때 리턴
                if (dataSnapshot.getValue() == null) {
                    return;

                    //맨위로 올라갔을때 리턴.
                } else if (dataSnapshot.getChildrenCount() == 1) {
                    Log.d("맨끝", "ㅇㅇㅇㅇㅇ");
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
                        comment.setKey(item.getKey());
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
//            messageReadUsers.put(uid, true);//메세지 작성시 읽음으로 표시 = true
            comment.uid = uid; // 채팅친사람
            comment.message = text; // 채팅친내용
            comment.timestamp = date.getTime(); // 채팅친 시간
            comment.type = "text"; // 채팅 친 종류
            comment.readUsers = messageReadUsers; // 읽은사람들
            comment.existUser = existUserGroupChat;

            new Description().execute(text);

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
                    Utiles.sendFcm(registration_ids, comment.message, GroupMessageActivity.this, toRoom);
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

    private void sendImg(Bitmap newBitmap, final int width, final int height, Uri uri) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        newBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bytes = baos.toByteArray();

        final ChatModel.Comment textComment = new ChatModel.Comment();
        textComment.uid = uid;
        textComment.message = width + "/" + height + "/" + String.valueOf(uri);
        textComment.timestamp = new Date().getTime();
        textComment.readUsers = messageReadUsers;
        textComment.type = "img";
        textComment.existUser = existUserGroupChat;
        newComments.add(textComment);
        mFirebaseAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - 1);

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
                    comment.message = width + "/" + height + "/" + imageUri;
                    comment.timestamp = date.getTime();
                    comment.readUsers = messageReadUsers;
                    comment.type = "img";
                    comment.existUser = existUserGroupChat;

                    newComments.remove(textComment);

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
                                    Utiles.sendFcm(registration_ids, "사진을 보냈습니다.", GroupMessageActivity.this, toRoom);
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

    private void sendThumnail(String uri) {
        if (inputStream == null) {
            Log.d("스트림없음", "dd");
            return;
        }
        final DatabaseReference userMessageKeyRef = databaseReference.child("groupChat").child(toRoom).child("comments").push();
        final String[] sp = uri.split("!!@@!!");
        final ChatModel.Comment textComment = new ChatModel.Comment();
        textComment.uid = uid;
        textComment.message = String.valueOf(sp[0]);
        textComment.timestamp = new Date().getTime();
        textComment.readUsers = messageReadUsers;
        textComment.type = sp[1] + "\n" + sp[2];
        textComment.existUser = existUserGroupChat;
        newComments.add(textComment);
        mFirebaseAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - 1);
        final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files").child(userMessageKeyRef.getKey());
        UploadTask uploadTask = storageReference.putStream(inputStream);

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
                    Uri taskResult = task.getResult();
                    String imageUri = taskResult.toString();
                    final ChatModel.Comment comment = new ChatModel.Comment();
                    comment.uid = uid;
                    comment.message = imageUri;
                    comment.timestamp = date.getTime();
                    comment.readUsers = messageReadUsers;
                    comment.type = sp[1] + "\n" + sp[2];
                    comment.existUser = existUserGroupChat;

                    newComments.remove(textComment);

                    databaseReference.child("groupChat").child(toRoom).child("comments").child(userMessageKeyRef.getKey()).setValue(comment);

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
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    //사진 사이즈(용량)조절
    private Map<String, Object> resize(Context context, Uri uri, int resize) {
        Bitmap resizeBitmap = null;
        Map<String, Object> map = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options); // 1번

            int width = options.outWidth;
            int height = options.outHeight;



            Log.d("사진크기", "width=" + width + "/height=" + height);
            int samplesize = 1;

            while (true) {//2번
                if (width / 2 < resize && height / 2 < resize) {
                    break;
                }

                width /= 2;
                height /= 2;
                samplesize *= 2;
            }
            Log.d("사진크기", "width=" + width + "/height=" + height + "/samplesize=" + samplesize);

            options.inSampleSize = samplesize;
            Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options); //3번
            resizeBitmap = bitmap;
            pWidth = width;
            pHeight = height;

            map = new HashMap<>();
            map.put("bitmap", resizeBitmap);
            map.put("width", width);
            map.put("height", height);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return map;
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
                            picture();
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

                    private void picture() {
                        Intent intent = new Intent(GroupMessageActivity.this, GalleryActivity.class);
                        Params params = new Params();
                        params.setCaptureLimit(1);
                        params.setPickerLimit(10);
                        params.setToolbarColor(R.id.dark);
                        params.setActionButtonColor(R.id.dark);
                        params.setButtonTextColor(R.id.dark);
                        intent.putExtra(Constants.KEY_PARAMS, params);
                        getIntent().putExtra("on", "on");
                        startActivityForResult(intent, 438);
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

        if (requestCode == 438 && resultCode == RESULT_OK) {


            if (!checker.equals("image")) {
                try {
                    fileUri = data.getData();
                    final String fileName = getFileName(fileUri);
                    final ChatModel.Comment fileComment = new ChatModel.Comment();
                    fileComment.uid = uid;
                    fileComment.message = "파일을 전송중입니다.";
                    fileComment.timestamp = new Date().getTime();
                    fileComment.readUsers = messageReadUsers;
                    fileComment.type = "text";
                    fileComment.existUser = existUserGroupChat;
                    newComments.add(fileComment);
                    mFirebaseAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - 1);


                    getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                    String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                    final DatabaseReference userMessageKeyRef = databaseReference.child("groupChat").child(toRoom).child("comments").push();
                    final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files").child(userMessageKeyRef.getKey() + "." + extension);
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

                                final Date date = new Date();
                                final List<String> registration_ids = new ArrayList<>();
                                Uri taskResult = task.getResult();
                                String fileUri = taskResult.toString();
                                final ChatModel.Comment comment = new ChatModel.Comment();
                                comment.uid = uid;
                                comment.message = fileName + fileUri;
                                comment.timestamp = date.getTime();
                                comment.readUsers = messageReadUsers;
                                comment.type = "file";
                                comment.existUser = existUserGroupChat;

                                newComments.remove(fileComment);

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
                                                Utiles.sendFcm(registration_ids, "파일을 보냈습니다.", GroupMessageActivity.this, toRoom);
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
                List<Image> imagesList = data.getParcelableArrayListExtra(Constants.KEY_BUNDLE_LIST);
                for (Image i : imagesList) {
                    Uri uri = i.uri;
                    if (createImgSize(uri)) return;

                }
            } else {
                Toast.makeText(this, "Error : Nothing Selected", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private boolean createImgSize(Uri uri) {
        String filePath;
        Map<String, Object> map = resize(this, uri, 1000);
        Bitmap bitmap = (Bitmap) map.get("bitmap");
        if (getIntent().getStringExtra("filePath") != null) {
            filePath = getIntent().getStringExtra("filePath");
        } else {
            filePath = getRealPathFromURI(uri);
        }
        if (filePath == null || filePath.equals("")) {
            Toast.makeText(GroupMessageActivity.this, "이미지를 보낼 수 없습니다.", Toast.LENGTH_SHORT).show();
            return true;
        }
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        Bitmap newBitmap = rotateBitmap(bitmap, orientation);
        int width = (int) map.get("width");
        int height = (int) map.get("height");
        sendImg(newBitmap, width, height, uri);
        return false;
    }

    private String getRealPathFromURI(Uri fileUri) {
        String result;
        Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
        if (cursor == null) {
            result = fileUri.getPath();
        } else {
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
                comment_modify.setKey(dataSnapshot.getKey());
                //화면에 뿌리는 코멘트.

                if(comment_modify.getType().equals("img")){
                    Img i = new Img();
                    i.setName(users.get(comment_modify.getUid()).getUserName());
                    String uri;
                    if(comment_modify.message.startsWith("http")){
                        uri = comment_modify.message;
                    }else{
                        int firstIndex = comment_modify.message.indexOf("/");
                        int secondIndex = comment_modify.message.indexOf("/", firstIndex + 1);
                        uri = comment_modify.message.substring(secondIndex + 1);
                    }
                    i.setUri(uri);
                    String time = String.valueOf((long)comment_modify.getTimestamp());
                    i.setTime(time);
                    img_list.add(i);
                }


                newComments.add(comment_modify);

                //commentCount = 처음 방에 진입했을 시점에 존재하는 총 코멘트 수
                //firstReadChatCount = 처음 불러오는 코멘트 개수
                //총 코멘트의 수가 처음 불러오는 코멘트 개수보다 적으면
                if (commentCount < firstReadChatCount) {
                    if (commentCount == c || commentCount + 1 == c) {
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
                        Log.d("키보드", newComments.size() - last + "");
                        if (newComments.size() - last > 2) {
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
                            } else if (newComments.get(newComments.size() - 1).type.equals("file")) {
                                messageText.setText("파일을 보냈습니다.");
                            } else if (newComments.get(newComments.size() - 1).type.equals("text")) {
                                messageText.setText(newComments.get(newComments.size() - 1).message);
                            } else {
                                messageText.setText("링크를 보냈습니다.");
                            }

                            relativeLayout.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    recyclerView.scrollToPosition(mFirebaseAdapter.getItemCount() - 1);
                                }
                            });
                            relativeLayout.setVisibility(View.VISIBLE);
                            mFirebaseAdapter.notifyDataSetChanged();
                        } else if (newComments.size() - last == 2) {
                            Log.d("키보드", "dd");
                            mFirebaseAdapter.notifyDataSetChanged();
                            if (newComments.get(newComments.size() - 2).message.length() < 550) {
                                recyclerView.scrollToPosition(last + 1);
                            }
                        } else {
                            mFirebaseAdapter.notifyDataSetChanged();
                        }
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
        //채팅방들어왓을때 알림 지우는부분.
        if (toRoom.equals("normalChat")) {
            chatName = "회원채팅방";
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
    protected void onPause() {
        super.onPause();
        recyclerView.removeOnLayoutChangeListener(onLayoutChangeListener);
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
        overridePendingTransition(R.anim.fromtop, R.anim.tobottom);//화면 사라지는 방향
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
            if (createImgSize(shareUri)) return;
        }
    }

    private class Description extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            String rText = strings[0].replaceAll(System.getProperty("line.separator"), " ");

            Log.d("나누기", rText);

            String[] sText = rText.split(" ");
            int checkLink = 0;
            for (int i = 0; i < sText.length; i++) {
                if (checkLink == 1) {
                    break;
                }
                if (!sText[i].equals("")) {
                    Log.d("나누기", sText[i]);
                    uriText = sText[i];
                    boolean b = Patterns.WEB_URL.matcher(uriText).matches();
                    if (b) {
                        checkLink++;
                        if (uriText.startsWith("www.")) {
                            uriText = "https://" + uriText;
                            Log.d("텍스트1", uriText);
                        } else if (uriText.startsWith("https://www.")) {
                            Log.d("텍스트2", uriText);
                        } else if (uriText.startsWith("https://")) {
                            Log.d("텍스트3", uriText);
                        } else if (uriText.startsWith("http://")) {
                            uriText = uriText.replace("http://", "https://");
                            Log.d("텍스트4", uriText);
                        } else {
                            uriText = "https://www." + uriText;
                            Log.d("텍스트5", uriText);
                        }
                    }
                }
            }
            try {
                String uri = null;
                String content = null;
                if (uriText.contains("youtube")) {
                    Log.d("유튜브", "dd");
                    int index = uriText.lastIndexOf("watch?v=");
                    uri = uriText.substring(index + 8);
                    String newUri = "https://img.youtube.com/vi/" + uri + "/0.jpg";
                    Log.d("유튜브", newUri);
                    URL url = new URL(newUri);
                    inputStream = (InputStream) url.getContent();
                    Document doc = Jsoup.connect(uriText).get();
                    Element tag = doc.selectFirst("title");
                    content = tag.html();
                    return newUri + "!!@@!!" + content + "!!@@!!" + uriText;
                } else {
                    Log.d("유튜브", "dss");
                    Document doc = Jsoup.connect(uriText).get();
                    Elements ogTags = doc.select("meta[property^=og:]");
                    if (ogTags.size() > 0) {
                        int imgC = 0;
                        int conC = 0;
                        for (int i = 0; i < ogTags.size(); i++) {
                            Element tag = ogTags.get(i);
                            String text = tag.attr("property");
                            if (text.equals("og:image")) {
                                if (imgC == 1) {
                                    continue;
                                }
                                imgC++;
                                uri = tag.attr("content");

                                if (!tag.attr("content").startsWith("https")) {
                                    if (tag.attr("content").startsWith("http")) {
                                        uri = tag.attr("content").replace("http", "https");
                                    } else {
                                        uri = "https:" + uri;
                                    }
                                }
                                Log.d("통과됨", uri + "");
                                URL url = new URL(uri);
                                inputStream = (InputStream) url.getContent();

                            } else if (text.equals("og:title")) {
                                if (conC == 1) {
                                    continue;
                                }
                                conC++;
                                content = tag.attr("content");
                                Log.d("통과됨 제목", content);
                            }
                        }
                        String splite = uri + "!!@@!!" + content + "!!@@!!" + uriText;
                        return splite;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            if (s != null) {
                Log.d("최종 보내는값", s);
                sendThumnail(s);
            }
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

            Log.d("타입", newComments.get(position).type);

            //채팅 안읽은 숫자 표시
            holder.textView_readCounter_left.setVisibility(View.INVISIBLE);
            holder.textView_readCounter_right.setVisibility(View.INVISIBLE);

            //상대 이름표시
            holder.textView_name.setVisibility(View.VISIBLE);

            holder.linearLayout_my.setVisibility(View.GONE);
            holder.linearLayout_other.setVisibility(View.GONE);

            //투표보낼때 쓰는 뷰
            holder.layout_vote.setVisibility(View.GONE);

            //이미지보낼때 쓰는 뷰
            holder.imageView.setVisibility(View.GONE);

            //텍스트보낼때 쓰는 뷰
            holder.textView_message.setVisibility(View.GONE);
            holder.textView_message.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            holder.textView_thumb.setVisibility(View.GONE);
            holder.textView_thumb_address.setVisibility(View.GONE);

            holder.layout_file.setVisibility(View.GONE);

            //메세지 보낸 시간.
            long unixTime = (long) newComments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            changeDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(metrics);

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
                    holder.textView_message.setMaxWidth(metrics.widthPixels - metrics.widthPixels / 3);
                    //복사 및 공유 기능
                    longClick(holder);

                    //이미지를 보냈을때
                } else if (newComments.get(position).type.equals("img")) {
                    drawImg(holder, position, metrics);

                } else if (newComments.get(position).type.equals("file")) {
                    String me = "me";
                    getFile(holder, position, me, metrics);
                } else if(newComments.get(position).type.equals("vote")){
                    getVote(holder, position);
                } else {
                    drawThumnail(holder, position, metrics);
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
                    holder.textView_message.setMaxWidth(metrics.widthPixels - metrics.widthPixels / 3);
                    longClick(holder);
                    //이미지 보냈을때
                } else if (newComments.get(position).type.equals("img")) {
                    drawImg(holder, position, metrics);
                } else if (newComments.get(position).type.equals("file")) {
                    String other = "other";
                    getFile(holder, position, other, metrics);
//                    longClick(holder);
                }else if(newComments.get(position).type.equals("vote")){
                    getVote(holder, position);
                } else {
                    drawThumnail(holder, position, metrics);
                }
                holder.linearlayout_main.setGravity(Gravity.LEFT);
                setReadCounter(position, holder.textView_readCounter_right);
                holder.linearLayout_other.setVisibility(View.VISIBLE);
                holder.textView_othertimestamp.setText(time);
            }
        }

        private void getVote(groupViewHolder holder, final int position) {
            holder.layout_vote.setVisibility(View.VISIBLE);
            holder.layout_vote.setBackgroundResource(R.drawable.receiver_messages_layout);
            holder.vote_title.setText(newComments.get(position).message.split("!@#!@#")[0]);
            holder.layout_vote.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final String vote_key = newComments.get(position).getKey();
                    Intent intent = new Intent(GroupMessageActivity.this, VoteActivity.class);
                    getIntent().putExtra("on","on");
                    intent.putExtra("key", vote_key);
                    intent.putExtra("room", toRoom);
                    long time = Long.parseLong(newComments.get(position).message.split("!@#!@#")[1]);

                    long today = new Date().getTime();

                    if(time-today<0){
                        intent.putExtra("flag", "over");
                    }else{
                        intent.putExtra("flag", "ing");
                    }
                    startActivity(intent);
                }
            });
        }

        private void drawThumnail(@NonNull groupViewHolder holder, int position, DisplayMetrics metrics) {
            holder.imageView.setVisibility(View.VISIBLE);
            holder.textView_thumb_address.setVisibility(View.VISIBLE);
            holder.textView_thumb.setVisibility(View.VISIBLE);
            holder.textView_thumb.setText(newComments.get(position).type);

            holder.textView_thumb.setBackgroundResource(R.drawable.thumb_messages_layout);
            LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) holder.imageView.getLayoutParams();
            LinearLayout.LayoutParams param2 = (LinearLayout.LayoutParams) holder.textView_thumb.getLayoutParams();
            param.width = metrics.widthPixels - metrics.widthPixels * 2 / 5;
            param.height = 2 * param.width / 3;
            param2.width = param.width;
            //이미지 처리하는 부분

            int u = newComments.get(position).message.lastIndexOf("https");
            int t = newComments.get(position).type.lastIndexOf("https");
            String thum = newComments.get(position).message.substring(u);
            String ad = newComments.get(position).type.substring(t);
            holder.textView_thumb_address.setText(ad);

            Glide.with(holder.itemView.getContext()).load(thum).placeholder(R.drawable.ic_base_img_24dp).apply(new RequestOptions().centerCrop()).into(holder.imageView);
            GradientDrawable gradientDrawable = (GradientDrawable) GroupMessageActivity.this.getDrawable(R.drawable.rectangle);
            holder.imageView.setBackground(gradientDrawable);
            holder.imageView.setClipToOutline(true);
            gradientDrawable = (GradientDrawable) GroupMessageActivity.this.getDrawable(R.drawable.rectangle_bottom);
            holder.textView_thumb_address.setBackground(gradientDrawable);
            int index = newComments.get(position).type.lastIndexOf("https://");
            String uri = newComments.get(position).type.substring(index);
            linkClick(holder.imageView, holder.textView_thumb, holder.textView_thumb_address, uri);
        }

        private void drawImg(@NonNull groupViewHolder holder, int position, DisplayMetrics metrics) {
            holder.imageView.setVisibility(View.VISIBLE);
            String uri;
            LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) holder.imageView.getLayoutParams();
            if (!newComments.get(position).message.startsWith("https")) {
                int firstIndex = newComments.get(position).message.indexOf("/");
                int secondIndex = newComments.get(position).message.indexOf("/", firstIndex + 1);
                int width = Integer.parseInt(newComments.get(position).message.substring(0, firstIndex));
                int height = Integer.parseInt(newComments.get(position).message.substring(firstIndex + 1, secondIndex));
                uri = newComments.get(position).message.substring(secondIndex + 1);

                //이미지 처리하는 부분
                if (width < metrics.widthPixels - metrics.widthPixels * 4 / 7) {
                    param.width = metrics.widthPixels - metrics.widthPixels * 4 / 7;
                    param.height = param.width * height / width;
                } else if (width >= metrics.widthPixels - metrics.widthPixels * 4 / 7 && width < metrics.widthPixels - metrics.widthPixels * 2 / 7) {
                    param.width = width;
                    param.height = height;
                } else {
                    param.width = metrics.widthPixels - metrics.widthPixels * 2 / 7;
                    param.height = param.width * height / width;
                }
            } else {
                param.width = metrics.widthPixels - metrics.widthPixels * 3 / 7;
                param.height = param.width;
                uri = newComments.get(position).message;
            }

            Glide.with(holder.itemView.getContext()).load(uri).placeholder(R.drawable.ic_base_img_24dp).override(param.width, param.height).into(holder.imageView);
            GradientDrawable gradientDrawable = (GradientDrawable) GroupMessageActivity.this.getDrawable(R.drawable.radius);
            holder.imageView.setBackground(gradientDrawable);
            holder.imageView.setClipToOutline(true);
            imgDownLoad(holder, uri, position);
            //공유 기능.
            imgLongClick(holder, uri);
        }

        private void autolink(TextView v, final boolean b) {
            v.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if (motionEvent.getAction() == MotionEvent.ACTION_UP) {

                        return b; // auto 막는거
                    }
                    return false;
                }
            });
        }

        private void linkClick(View v, View v2, View v3, final String uri) {
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                }
            });
            v2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                }
            });
            v3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                }
            });
        }

        private void getFile(@NonNull groupViewHolder holder, final int position, String identify, DisplayMetrics metrics) {

            holder.layout_file.setVisibility(View.VISIBLE);
            final String message = newComments.get(position).message;
            final int lastIndex = message.lastIndexOf("https://");

            String[] extension = message.substring(0, lastIndex).split("\\.");
            final String ext = extension[extension.length - 1];
            String file = "종류 : " + ext;
            holder.layout_file_name.setText(message.substring(0, lastIndex));
            holder.layout_file_name.setMaxWidth(metrics.widthPixels - metrics.widthPixels * 3 / 5);
            holder.layout_file_extension.setText(file);

            holder.layout_file.setClickable(true);
            holder.layout_file.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (message.contains(".pdf")) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            intent.setDataAndType(Uri.parse(message.substring(lastIndex)), "application/pdf");
                            startActivity(intent);
                        } else {

                            //외부앱 파일 접근할시 fileprovider써야함. 외부sd저장소 등 접근하려면 file_path.xml에 root설정.

                            File path = Environment.getExternalStorageDirectory();
                            File dir = new File(path + "/KCHA/DownloadFile");
                            dir.mkdirs();
                            String filename = message.substring(0, lastIndex);
                            final File file = new File(dir, filename);
                            FirebaseStorage.getInstance().getReference().child("Document Files/" + newComments.get(position).key + "." + ext)
                                    .getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    Toast.makeText(GroupMessageActivity.this, "파일을 다운받았습니다.", Toast.LENGTH_SHORT).show();
                                    try {
                                        Uri uri = FileProvider.getUriForFile(GroupMessageActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
                                        Intent intent = new Intent(Intent.ACTION_VIEW);
                                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        if (ext.equals("hwp")) {
                                            intent.setDataAndType(uri, "application/haansofthwp");
                                        } else if (ext.contains("xls")) {
                                            intent.setDataAndType(uri, "application/vnd.ms-excel");
                                        } else {
                                            intent.setDataAndType(uri, "application/*");
                                        }
                                        startActivity(intent);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(GroupMessageActivity.this, "설치된 뷰어가 없어 파일을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(GroupMessageActivity.this, "파일을 받을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(GroupMessageActivity.this, "파일을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            holder.layout_file.setBackgroundResource(R.drawable.receiver_messages_layout);
        }

        private void imgDownLoad(final groupViewHolder holder, final String message, final int position) {
            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Log.d("사진목록", img_list.toString());

                    Intent intent = new Intent(GroupMessageActivity.this, BigPictureActivity.class);
                    long times = (long) newComments.get(position).timestamp;
                    String find = String.valueOf(times);
                    Img im = new Img();
                    im.setTime(find);
                    int position2 = img_list.indexOf(im);
                    Log.d("사진목록", find+"");
                    Log.d("사진목록", position2+"");
                    intent.putExtra("position", position2);
                    intent.putExtra("uri", message);
                    intent.putParcelableArrayListExtra("imglist", (ArrayList<? extends Parcelable>) img_list);
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
                Glide.with(holder.itemView.getContext()).load(users.get(newComments.get(position).uid).getUserProfileImageUrl()).placeholder(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(holder.imageView_profile);
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
                                File path = getCacheDir();
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
                                    if (file.exists()) {
                                        file.deleteOnExit();
                                    }
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
                    final TextView tv = (TextView) view;
                    autolink(tv, true);
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
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            autolink(tv, false);
                        }
                    });
                    builder.show();
                    return true;
                }
            });
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
                if (userNumber > 99) {
                    textView.setText("99");
                } else {
                    textView.setText(String.valueOf(userNumber));
                }
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
            private LinearLayout item_message, layout_file, layout_vote;
            private TextView textView_thumb, vote_title;
            private TextView textView_thumb_address, layout_file_name, layout_file_extension;

            private groupViewHolder(@NonNull View view) {
                super(view);
                layout_vote = view.findViewById(R.id.layout_vote);
                vote_title = view.findViewById(R.id.vote_title);
                item_message = view.findViewById(R.id.item_message);
                layout_file = view.findViewById(R.id.layout_file);
                layout_file_name = view.findViewById(R.id.layout_file_name);
                layout_file_extension = view.findViewById(R.id.layout_file_extension);

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

                textView_thumb = view.findViewById(R.id.messageItem_textView_thumbnail);
                textView_thumb_address = view.findViewById(R.id.messageItem_textView_thumbnail_address);
            }
        }
    }
}
