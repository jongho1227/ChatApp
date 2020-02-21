package com.onvit.chatapp.admin;


import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.onvit.chatapp.model.User;

import java.util.ArrayList;
import java.util.List;

public class SignPageAdapter extends FragmentStatePagerAdapter { // 프래그먼트 변화시켜야 할때 이거 상속받고 맨아래있는거 오버라이드.
    List<Fragment> mList;
    List<User> signUser;
    List<User> unSignUser;
    public SignPageAdapter(@NonNull FragmentManager fm, final int a, List<User> signUser, List<User> UnSignUser) {
        super(fm, a);
        mList = new ArrayList<>();

        final SignedFragment signedFragment = new SignedFragment();
        final UnsignedFragment unsignedFragment = new UnsignedFragment();

        this.signUser = signUser;
        this.unSignUser = UnSignUser;

        final Bundle sign = new Bundle();
        final Bundle unSign = new Bundle();

        sign.putParcelableArrayList("sign", (ArrayList<? extends Parcelable>) signUser);
        unSign.putParcelableArrayList("unSign", (ArrayList<? extends Parcelable>) unSignUser);

        signedFragment.setArguments(sign);
        unsignedFragment.setArguments(unSign);
        mList.add(signedFragment);
        mList.add(unsignedFragment);
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
            return "가입자 : "+signUser.size();
        } else if (position == 1) {
            return "미가입자 : "+unSignUser.size() ;
        }
        return null;
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return super.getItemPosition(object);
    }
}
