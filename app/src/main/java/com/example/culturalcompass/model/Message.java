package com.example.culturalcompass.model;

public class Message {
    public static final int USER = 0;
    public static final int ASSISTANT = 1;

    public int role;
    public String text;

    public Message(int role, String text) {
        this.role = role;
        this.text = text;
    }
}
