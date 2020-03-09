package com.onvit.chatapp.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//마지막채팅내용 및 각자 안읽은 메세지 개수 표시.
public class LastChat implements Comparable<LastChat>{
    private Map<String, Integer> users = new HashMap<>();
    private Map<String, Boolean> existUsers = new HashMap<>();
    private String chatName;
    private String lastChat;
    private long timestamp;
    private String photo;

    public LastChat() {
    }

    public Map<String, Boolean> getExistUsers() {
        return existUsers;
    }

    public void setExistUsers(Map<String, Boolean> existUsers) {
        this.existUsers = existUsers;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }

    public Map<String, Integer> getUsers() {
        return users;
    }

    public void setUsers(Map<String, Integer> users) {
        this.users = users;
    }

    public String getLastChat() {
        return lastChat;
    }

    public void setLastChat(String lastChat) {
        this.lastChat = lastChat;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }


    @Override
    public String toString() {
        return "LastChat{" +
                "users=" + users +
                ", chatName='" + chatName + '\'' +
                ", lastChat='" + lastChat + '\'' +
                ", timestamp=" + timestamp +
                ", photo='" + photo + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LastChat lastChat = (LastChat) o;
        return Objects.equals(chatName, lastChat.chatName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatName);
    }

    @Override
    public int compareTo(LastChat lastChat) {
        if(this.timestamp - lastChat.getTimestamp()>0){
            return -1;
        }else if(this.timestamp - lastChat.getTimestamp()<0){
            return 1;
        }
        return 0;
    }
}
