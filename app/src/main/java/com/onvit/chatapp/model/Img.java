package com.onvit.chatapp.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

public class Img implements Parcelable {
    public static final Creator<Img> CREATOR = new Creator<Img>() {
        @Override
        public Img createFromParcel(Parcel in) {
            return new Img(in);
        }

        @Override
        public Img[] newArray(int size) {
            return new Img[size];
        }
    };
    private String name;
    private String uri;
    private String time;

    public Img() {
    }

    protected Img(Parcel in) {
        name = in.readString();
        uri = in.readString();
        time = in.readString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Img img = (Img) o;
        return Objects.equals(time, img.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(uri);
        parcel.writeString(time);
    }

    @Override
    public String toString() {
        return "Img{" +
                "name='" + name + '\'' +
                ", uri='" + uri + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}