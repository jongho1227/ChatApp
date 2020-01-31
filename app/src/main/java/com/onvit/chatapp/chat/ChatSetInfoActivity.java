package com.onvit.chatapp.chat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
import com.onvit.chatapp.R;
import com.onvit.chatapp.contact.PersonInfoActivity;
import com.onvit.chatapp.model.User;

import java.util.ArrayList;

public class ChatSetInfoActivity extends Activity {
    LinearLayout chat_info_linear_layout;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_set_info);

        chat_info_linear_layout = findViewById(R.id.chat_info_linear_layout);
        recyclerView = findViewById(R.id.peopleinfo_recyclerview);

        ArrayList<User> userlist = getIntent().getParcelableArrayListExtra("userInfo");
        WindowManager.LayoutParams wmlp = getWindow().getAttributes();
        wmlp.gravity = Gravity.TOP | Gravity.END;

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);


        int width, height;

        width = wm.getDefaultDisplay().getWidth();
        height = wm.getDefaultDisplay().getHeight();

        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            height -= getResources().getDimensionPixelSize(resourceId) / 2;
        }
        chat_info_linear_layout.getLayoutParams().width = (int) (width * 0.75);
        chat_info_linear_layout.getLayoutParams().height = height;

        this.setFinishOnTouchOutside(true);


        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PeopleInfoRecyclerAdapter(userlist));

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
