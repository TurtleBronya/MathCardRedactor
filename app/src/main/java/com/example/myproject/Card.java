package com.example.myproject;

import java.util.List;

public class Card {
    public long id;
    public String title;
    public String question;
    public String answer;
    public long createdAt;
    public long updatedAt;
    public List<CardItem> items;

    public Card() {
        this.items = new java.util.ArrayList<>();
    }
}