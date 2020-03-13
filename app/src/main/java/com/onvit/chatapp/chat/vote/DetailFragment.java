package com.onvit.chatapp.chat.vote;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.onvit.chatapp.R;
import com.onvit.chatapp.model.User;
import com.onvit.chatapp.model.Vote;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DetailFragment extends Fragment {
    Map<String, List<User>> user;
    List<Vote> voteList;

    public DetailFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sign_fragment, container, false);
        user = (Map<String, List<User>>) getArguments().getSerializable("detail");
        voteList = new ArrayList<>();
        Set<String> key = user.keySet();
        Iterator<String> its = key.iterator();
        while (its.hasNext()) {
            Vote v = new Vote();
            String k = its.next();
            v.setTitle(k);
            v.setList(user.get(k));
            voteList.add(v);
        }
        RecyclerView recyclerView = view.findViewById(R.id.peoplefragment_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        DetailRecyclerAdapter pf = new DetailRecyclerAdapter(voteList);
        recyclerView.setAdapter(pf);
        return view;
    }

    class DetailRecyclerAdapter extends RecyclerView.Adapter<DetailRecyclerAdapter.CustomViewHolder> {
        List<Vote> voteList;

        public DetailRecyclerAdapter(List<Vote> voteList) {
            this.voteList = voteList;
        }

        @NonNull
        @Override
        public CustomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.detail_layout, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomViewHolder holder, final int position) {
            holder.subject.setText(position+1 +"."+ voteList.get(position).getTitle());
            holder.recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            InnerRecyclerAdapter innerRecyclerAdapter = new InnerRecyclerAdapter(voteList.get(position).getList());
            holder.recyclerView.setAdapter(innerRecyclerAdapter);
            innerRecyclerAdapter.notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return voteList.size();
        }


        private class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView subject;
            RecyclerView recyclerView;
            public CustomViewHolder(View view) {
                super(view);
                subject = view.findViewById(R.id.subject);
                recyclerView = view.findViewById(R.id.recycler_view);

            }
        }


        class InnerRecyclerAdapter extends RecyclerView.Adapter<InnerRecyclerAdapter.Custom2ViewHolder> {
            List<User> list;

            public InnerRecyclerAdapter(List<User> list) {
                this.list = list;
            }

            @NonNull
            @Override
            public Custom2ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_small, parent, false);
                return new Custom2ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull Custom2ViewHolder holder, final int position) {
                //사진에 곡률넣음.
                if(list.get(position).getUserProfileImageUrl().equals("noImg")){
                    Glide.with(holder.itemView.getContext()).load(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(holder.imageView);
                }else{
                    Glide.with(holder.itemView.getContext()).load(list.get(position).getUserProfileImageUrl()).placeholder(R.drawable.standard_profile).apply(new RequestOptions().centerCrop()).into(holder.imageView);
                }
                GradientDrawable gradientDrawable = (GradientDrawable) getContext().getDrawable(R.drawable.radius);
                holder.imageView.setBackground(gradientDrawable);
                holder.imageView.setClipToOutline(true);

                holder.textView.setText(list.get(position).getUserName());

                holder.textView_hospital.setText("["+list.get(position).getHospital()+"]");

            }

            @Override
            public int getItemCount() {
                return list.size();
            }


            private class Custom2ViewHolder extends RecyclerView.ViewHolder {
                public ImageView imageView;
                public TextView textView;
                public TextView textView_hospital;
                public TextView lineText;

                public Custom2ViewHolder(View view) {
                    super(view);
                    imageView = view.findViewById(R.id.frienditem_imageview);
                    textView = view.findViewById(R.id.frienditem_textview);
                    textView_hospital = view.findViewById(R.id.frienditem_textview_hospital);
                    lineText = view.findViewById(R.id.line_text);
                }
            }
        }
    }
}