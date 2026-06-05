package com.example.myproject;

import java.util.ArrayList;
import java.util.List;

public class Deck {
    public long id;
    public String title;
    public long createdAt;
    public long updatedAt;
    public List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }
}