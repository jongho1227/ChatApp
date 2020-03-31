package com.onvit.chatapp.chat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.onvit.chatapp.BuildConfig;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.ChatModel;
import com.onvit.chatapp.util.Utiles;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FileActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FileRecyclerAdapter fileRecyclerAdapter;
    private List<ChatModel.Comment> list = new ArrayList<>();
    private String uid;
    private String toRoom;
    private Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        activity = FileActivity.this;
        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundResource(R.color.notice);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        String chatName;
        toRoom = getIntent().getStringExtra("room");
        if (toRoom.equals("normalChat")) {
            chatName = "회원채팅방 파일목록";
        } else if (toRoom.equals("officerChat")){
            chatName = "임원채팅방 파일목록";
        } else{
            chatName = toRoom + " 파일목록";
        }
        actionBar.setTitle(chatName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance().getReference().child("groupChat").child(toRoom)
                .child("comments").orderByChild("type").equalTo("file").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot item : dataSnapshot.getChildren()) {
                    ChatModel.Comment comment = item.getValue(ChatModel.Comment.class);
                    comment.setKey(item.getKey());
                    list.add(comment);
                }
                recyclerView = findViewById(R.id.file_recycler);
                recyclerView.setLayoutManager(new GridLayoutManager(FileActivity.this, 2));
                fileRecyclerAdapter = new FileRecyclerAdapter();
                recyclerView.setAdapter(fileRecyclerAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    //뒤로가기 눌렀을때
    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromright, R.anim.toleft);//화면 사라지는 방향
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

    @Override
    protected void onResume() {
        super.onResume();
        Map<String, Object> map = new HashMap<>();
        map.put(uid, true);
        FirebaseDatabase.getInstance().getReference().child("groupChat").child(toRoom).child("users").updateChildren(map);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (getIntent().getStringExtra("on") == null) {
            Map<String, Object> map = new HashMap<>();
            map.put(uid, false);
            FirebaseDatabase.getInstance().getReference().child("groupChat").child(toRoom).child("users").updateChildren(map);
        }
    }

    class FileRecyclerAdapter extends RecyclerView.Adapter<FileRecyclerAdapter.FileViewHolder> {


        public FileRecyclerAdapter() {

        }

        @NonNull
        @Override
        public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
            return new FileViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final FileViewHolder holder, final int position) {

            SimpleDateFormat sd = new SimpleDateFormat("yyyy년 MM월 dd일");

            long d = (long) list.get(position).timestamp;

            Date dd = new Date(d);

            String date = sd.format(dd);
            holder.date.setText(date);
            final String[] fileName = list.get(position).message.split("http");
            holder.text.setText(fileName[0]);
            String[] extension = fileName[0].split("\\.");
            final String ext = extension[extension.length - 1];
            holder.itemView.setClickable(true);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        if (fileName[0].contains(".pdf")) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            intent.setDataAndType(Uri.parse("http" + fileName[1]), "application/pdf");
                            startActivity(intent);
                        } else {
                            //외부앱 파일 접근할시 fileprovider써야함. 외부sd저장소 등 접근하려면 file_path.xml에 root설정.
                            File path = Environment.getExternalStorageDirectory();
                            File dir = new File(path + "/대한지역병원협의회/DownloadFile");
                            dir.mkdirs();
                            String filename = fileName[0];
                            final File file = new File(dir, filename);
                            FirebaseStorage.getInstance().getReference().child("Document Files/" +toRoom+"/"+ list.get(position).key + "." + ext)
                                    .getFile(file).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    if(!activity.isDestroyed()){
                                        try {
                                            Uri uri = FileProvider.getUriForFile(FileActivity.this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
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
                                            Utiles.customToast(FileActivity.this, "설치된 뷰어가 없어 파일을 열 수 없습니다.").show();
                                        }
                                        Utiles.customToast(FileActivity.this, "파일을 다운받았습니다.").show();
                                    }
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    if(!activity.isDestroyed()){
                                        Utiles.customToast(FileActivity.this, "파일을 받을 수 없습니다.").show();
                                    }
                                }
                            });
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        if(!activity.isDestroyed()){
                            Utiles.customToast(FileActivity.this, "파일을 열 수 없습니다.").show();
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }


        private class FileViewHolder extends RecyclerView.ViewHolder {
            TextView text, date;

            public FileViewHolder(View v) {
                super(v);
                text = v.findViewById(R.id.file_name);
                date = v.findViewById(R.id.date);
            }
        }
    }
}
