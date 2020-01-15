package com.onvit.chatapp.model;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Comparable<User>, Parcelable {
    private String userName;
    private String userEmail;
    private String userPassword;
    private String userProfileImageUrl;
    private String uid;
    private String pushToken;
    private String tel;
    private String hospital;
    private String grade;

    public User(){

    }

    protected User(Parcel in) {
        userName = in.readString();
        userEmail = in.readString();
        userPassword = in.readString();
        userProfileImageUrl = in.readString();
        uid = in.readString();
        pushToken = in.readString();
        tel = in.readString();
        hospital = in.readString();
        grade = in.readString();
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

    @Override
    public String toString() {
        return "User{" +
                "userName='" + userName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", userPassword='" + userPassword + '\'' +
                ", userProfileImageUrl='" + userProfileImageUrl + '\'' +
                ", uid='" + uid + '\'' +
                ", pushToken='" + pushToken + '\'' +
                ", tel='" + tel + '\'' +
                ", hospital='" + hospital + '\'' +
                ", grade='" + grade + '\'' +
                '}';
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
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
        parcel.writeString(tel);
        parcel.writeString(hospital);
        parcel.writeString(grade);
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
