package com.example.myproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeckActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private DatabaseHelper dbHelper;
    private long deckId;
    private String deckTitle;
    private TextView tvDeckTitle;
    private ImageButton btnEditDeck;
    private Button btnSolveDeck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deck);

        dbHelper = new DatabaseHelper(this);

        deckId = getIntent().getLongExtra("deck_id", -1);
        deckTitle = getIntent().getStringExtra("deck_title");

        if (deckId == -1) {

            finish();
            return;
        }

        tvDeckTitle = findViewById(R.id.tvDeckTitle);
        recyclerView = findViewById(R.id.recyclerView);
        btnEditDeck = findViewById(R.id.btnEditDeck);
        btnSolveDeck = findViewById(R.id.btnSolveDeck);

        tvDeckTitle.setText(deckTitle);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnEditDeck.setOnClickListener(v -> openEditDeckActivity());
        btnSolveDeck.setOnClickListener(v -> openReadCardActivity());

        loadCards();
    }

    private void openEditDeckActivity() {
        Intent intent = new Intent(this, CreateDeckActivity.class);
        intent.putExtra("deck_id", deckId);
        intent.putExtra("deck_title", deckTitle);
        intent.putExtra("is_edit_mode", true);
        startActivity(intent);
    }

    private void openReadCardActivity() {
        List<Card> cards = dbHelper.getCardsByDeckId(deckId);
        if (cards.isEmpty()) {

            return;
        }

        Intent intent = new Intent(this, ReadCardActivity.class);
        intent.putExtra("deck_id", deckId);
        startActivity(intent);
    }

    private void loadCards() {
        List<Card> cards = dbHelper.getCardsByDeckId(deckId);
        updateAdapter(cards);
    }

    private void updateAdapter(List<Card> cards) {
        if (cardAdapter == null) {
            cardAdapter = new CardAdapter(cards,
                    card -> {

                    },
                    card -> {
                        Intent intent = new Intent(this, RedactCardActivity.class);
                        intent.putExtra("card_id", card.id);
                        intent.putExtra("card_title", card.title);
                        intent.putExtra("card_question", card.question);
                        intent.putExtra("card_answer", card.answer);
                        startActivity(intent);
                    },
                    card -> {
                        boolean removed = dbHelper.removeCardFromDeck(deckId, card.id);
                        if (removed) {
                            loadCards();

                        }
                    }
            );
            recyclerView.setAdapter(cardAdapter);
        } else {
            cardAdapter.updateCards(cards);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCards();
        List<Deck> decks = dbHelper.getAllDecks();
        for (Deck deck : decks) {
            if (deck.id == deckId) {
                deckTitle = deck.title;
                tvDeckTitle.setText(deckTitle);
                break;
            }
        }
    }
}