package com.onvit.chatapp.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.onvit.chatapp.R;
import com.onvit.chatapp.chat.vote.VoteListActivity;
import com.onvit.chatapp.contact.PersonInfoActivity;
import com.onvit.chatapp.model.Img;
import com.onvit.chatapp.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatSetInfoActivity extends Activity implements View.OnClickListener {
    LinearLayout chat_info_linear_layout;
    RecyclerView recyclerView;
    TextView vote, file, img;
    private String uid;
    private String toRoom;
    private List<Img> img_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_set_info);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        toRoom = getIntent().getStringExtra("room");
        chat_info_linear_layout = findViewById(R.id.chat_info_linear_layout);
        recyclerView = findViewById(R.id.peopleinfo_recyclerview);
        vote = findViewById(R.id.vote);
        file = findViewById(R.id.file);
        img = findViewById(R.id.img);
        ArrayList<User> userlist = getIntent().getParcelableArrayListExtra("userInfo");
        img_list = getIntent().getParcelableArrayListExtra("imglist");
        Log.d("유저정보", userlist.toString());

        WindowManager.LayoutParams wmlp = getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP | Gravity.END;

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);


        int width, height;

        width = metrics.widthPixels;
        height = metrics.heightPixels;

        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height -= getResources().getDimensionPixelSize(resourceId) / 2;
        }
        chat_info_linear_layout.getLayoutParams().width = (int) (width * 0.75);
        chat_info_linear_layout.getLayoutParams().height = height;

        this.setFinishOnTouchOutside(true);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PeopleInfoRecyclerAdapter(userlist));

        vote.setOnClickListener(this);
        file.setOnClickListener(this);
        img.setOnClickListener(this);
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.fromleft, R.anim.toright);//화면 사라지는 방향
    }

    @Override
    protected void onResume() {
        super.onResume();
        chat_info_linear_layout.setBackgroundColor(Color.WHITE);
        getIntent().removeExtra("on");
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

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.vote:
                intent = new Intent(ChatSetInfoActivity.this, VoteListActivity.class);
                intent.putExtra("room", getIntent().getStringExtra("room"));
                getIntent().putExtra("on", "on");
                startActivity(intent);
                overridePendingTransition(R.anim.fromleft, R.anim.toright);
                break;
            case R.id.file:
                intent = new Intent(ChatSetInfoActivity.this, FileActivity.class);
                intent.putExtra("room", getIntent().getStringExtra("room"));
                getIntent().putExtra("on", "on");
                startActivity(intent);
                overridePendingTransition(R.anim.fromleft, R.anim.toright);
                break;
            case R.id.img:
                intent = new Intent(ChatSetInfoActivity.this, ImgActivity.class);
                intent.putExtra("room", getIntent().getStringExtra("room"));
                intent.putParcelableArrayListExtra("imglist", (ArrayList<? extends Parcelable>) img_list);
                getIntent().putExtra("on", "on");
                startActivity(intent);
                overridePendingTransition(R.anim.fromleft, R.anim.toright);
                break;
        }
    }

    class PeopleInfoRecyclerAdapter extends RecyclerView.Adapter<PeopleInfoRecyclerAdapter.CustomViewHolder> {
        ArrayList<User> userlist;

        public PeopleInfoRecyclerAdapter(ArrayList<User> userlist) {
            this.userlist = userlist;
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_small, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewHolder holder, final int position) {
            Log.d("홀더붙는순서(연락처)", position + "");
            //position0번 부터 붙음

            holder.lineText.setVisibility(View.GONE);
            if (position == 1) {// 본인이랑 다음사람이랑 구분선.
                holder.lineText.setVisibility(View.VISIBLE);
            }
            //사진에 곡률넣음.
            if (userlist.get(position).getUserProfileImageUrl().equals("noImg")) {
                Glide.with(holder.itemView.getContext()).load(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(holder.imageView);
            } else {
                Glide.with(holder.itemView.getContext()).load(userlist.get(position).getUserProfileImageUrl()).placeholder(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(holder.imageView);
            }
            GradientDrawable gradientDrawable = (GradientDrawable) ChatSetInfoActivity.this.getDrawable(R.drawable.radius);
            holder.imageView.setBackground(gradientDrawable);
            holder.imageView.setClipToOutline(true);

            holder.textView.setText(userlist.get(position).getUserName());

            holder.textView_hospital.setText("[" + userlist.get(position).getHospital() + "]");

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ChatSetInfoActivity.this, PersonInfoActivity.class);
                    intent.putExtra("info", userlist.get(position).getUid());
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return userlist.size();
        }


        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public TextView textView_hospital;
            public TextView lineText;

            public CustomViewHolder(View view) {
                super(view);
                imageView = view.findViewById(R.id.frienditem_imageview);
                textView = view.findViewById(R.id.frienditem_textview);
                textView_hospital = view.findViewById(R.id.frienditem_textview_hospital);
                lineText = view.findViewById(R.id.line_text);
            }
        }
    }
}
