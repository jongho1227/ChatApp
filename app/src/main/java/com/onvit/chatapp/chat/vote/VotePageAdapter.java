package com.onvit.chatapp.chat.vote;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.onvit.chatapp.model.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VotePageAdapter extends FragmentStatePagerAdapter {// 프래그먼트 변화시켜야 할때 이거 상속받고 맨아래있는거 오버라이드.
    List<Fragment> mList;
    List<User> joinUser;
    List<User> notJoinUser;
    Map<String, List<User>> detailUserMap;
    public VotePageAdapter(@NonNull FragmentManager fm, final int a, List<User> joinUser, List<User> notJoinUser, Map<String, List<User>> detailUserMap) {
        super(fm, a);
        mList = new ArrayList<>();

        final JoinFragment joinFragment = new JoinFragment();
        final NotJoinFragment notJoinFragment = new NotJoinFragment();
        final DetailFragment detailFragment = new DetailFragment();

        this.joinUser = joinUser;
        this.notJoinUser = notJoinUser;
        this.detailUserMap = detailUserMap;


        final Bundle join = new Bundle();
        final Bundle notJoin = new Bundle();
        final Bundle detail = new Bundle();

        join.putParcelableArrayList("join", (ArrayList<? extends Parcelable>) joinUser);
        notJoin.putParcelableArrayList("notJoin", (ArrayList<? extends Parcelable>) notJoinUser);
        detail.putSerializable("detail", (Serializable) detailUserMap);

        joinFragment.setArguments(join);
        notJoinFragment.setArguments(notJoin);
        detailFragment.setArguments(detail);

        mList.add(joinFragment);
        mList.add(notJoinFragment);
        mList.add(detailFragment);
    }


    @NonNull
    @Override
    public Fragment getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (position == 0) {
            return "참여자 : "+joinUser.size();
        } else if (position == 1) {
            return "미참여자 : "+notJoinUser.size();
        }
        else if (position == 2){
            return "항목별";
        }
        return null;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return super.getItemPosition(object);
    }
}
