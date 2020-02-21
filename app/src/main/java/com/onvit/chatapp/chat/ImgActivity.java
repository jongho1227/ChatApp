package com.onvit.chatapp.chat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.Img;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImgActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private String toRoom;
    private String uid;
    private List<Img> img_list;
    private RecyclerView recyclerView;
    private ImgRecyclerAdapter imgRecyclerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setBackgroundResource(R.color.notice);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        String chatName;
        toRoom = getIntent().getStringExtra("room");
        if (toRoom.equals("normalChat")) {
            chatName = "회원채팅방 이미지목록";
        } else {
            chatName = "임원채팅방 이미지목록";
        }
        actionBar.setTitle(chatName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        img_list = getIntent().getParcelableArrayListExtra("imglist");
        if(img_list==null){
            img_list = new ArrayList<>();
        }
        Collections.reverse(img_list);
        recyclerView = findViewById(R.id.img_recycler);
        recyclerView.setLayoutManager(new GridLayoutManager(ImgActivity.this, 3));
        imgRecyclerAdapter = new ImgRecyclerAdapter();
        recyclerView.setAdapter(imgRecyclerAdapter);
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

    class ImgRecyclerAdapter extends RecyclerView.Adapter<ImgRecyclerAdapter.ImgViewHolder> {


        public ImgRecyclerAdapter() {

        }

        @NonNull
        @Override
        public ImgViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_img, parent, false);
            return new ImgViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final ImgViewHolder holder, final int position) {
            Glide.with(ImgActivity.this).load(img_list.get(position).getUri()).apply(new RequestOptions().centerCrop()).into(holder.imageView);
            holder.imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ImgActivity.this, BigPictureActivity.class);
                    String find = img_list.get(position).getTime();
                    Img im = new Img();
                    im.setTime(find);
                    int position2 = img_list.indexOf(im);
                    intent.putExtra("position", position2);
                    intent.putExtra("uri", img_list.get(position).getUri());
                    intent.putParcelableArrayListExtra("imglist", (ArrayList<? extends Parcelable>) img_list);
                    intent.putExtra("name", img_list.get(position).getName());
                    getIntent().putExtra("on", "on");
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return img_list.size();
        }


        private class ImgViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ImgViewHolder(View v) {
                super(v);
                imageView = v.findViewById(R.id.img);
            }
        }
    }
}
