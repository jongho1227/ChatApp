package com.onvit.chatapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class User implements Comparable<User>, Parcelable {
    private String userName;
    private String userEmail;
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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(userName);
        parcel.writeString(userEmail);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userName, user.userName) &&
                Objects.equals(tel, user.tel) || Objects.equals(uid, user.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, tel);
    }
}
