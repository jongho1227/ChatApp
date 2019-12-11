package com.example.chatapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

import io.realm.RealmObject;

public class User implements Comparable<User>, Parcelable {
    private String userName;
    private String userEmail;
    private String userPassword;
    private String userProfileImageUrl;
    private String uid;
    private String pushToken;
    private String comment;
    private String tel;

    public User(){

    }

    protected User(Parcel in) {
        userName = in.readString();
        userEmail = in.readString();
        userPassword = in.readString();
        userProfileImageUrl = in.readString();
        uid = in.readString();
        pushToken = in.readString();
        comment = in.readString();
        tel = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPushToken() {
        return pushToken;
    }

    public void setPushToken(String pushToken) {
        this.pushToken = pushToken;
    }

    public String getUserProfileImageUrl() {
        return userProfileImageUrl;
    }

    public void setUserProfileImageUrl(String userProfileImageUrl) {
        this.userProfileImageUrl = userProfileImageUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", userPassword='" + userPassword + '\'' +
                ", userProfileImageUrl='" + userProfileImageUrl + '\'' +
                ", uid='" + uid + '\'' +
                ", pushToken='" + pushToken + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(userName);
        parcel.writeString(userEmail);
        parcel.writeString(userPassword);
        parcel.writeString(userProfileImageUrl);
        parcel.writeString(uid);
        parcel.writeString(pushToken);
        parcel.writeString(comment);
        parcel.writeString(tel);
    }

    @Override
    public int compareTo(User user) {
        if(this.userName.compareTo(user.getUserName())>0){
            return 1;
        }else if(this.userName.compareTo(user.getUserName())<0){
            return -1;
        }
        return 0;
    }
}
