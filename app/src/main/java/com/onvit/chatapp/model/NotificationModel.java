package com.onvit.chatapp.model;

import java.util.List;

public class NotificationModel {

    public List<String> registration_ids;
    //    public Notification notification = new Notification();
    public Data data = new Data();
    public boolean content_available;
    public String priority;
    public int time_to_live;
    public boolean delay_while_idle;

//    public static class Notification {
//        public String title;
//        public String text;
//        public String tag;
//        public String click_action;
//    }

    public static class Data {
        public String title;
        public String body;
        public String tag;
        public String click_action;
    }

}
