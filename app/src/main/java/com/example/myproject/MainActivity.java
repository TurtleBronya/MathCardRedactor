package com.example.myproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private DeckAdapter deckAdapter;
    private DatabaseHelper dbHelper;
    private EditText searchBar;
    private FloatingActionButton fabCreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchBar = findViewById(R.id.Searchbar);
        fabCreate = findViewById(R.id.fabCreate);

        setupSearch();
        setupFab();
        loadDecks();
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFab() {
        fabCreate.setOnClickListener(v -> showCreateDeckDialog());
    }

    private void performSearch(String query) {
        List<Deck> decks = dbHelper.searchDecks(query);

        // Загружаем карточки для каждой колоды
        for (Deck deck : decks) {
            deck.cards = dbHelper.getCardsByDeckId(deck.id);
        }

        updateAdapter(decks);
    }

    private void loadDecks() {
        performSearch("");
    }

    private void updateAdapter(List<Deck> decks) {
        if (deckAdapter == null) {
            deckAdapter = new DeckAdapter(decks,
                    deck -> {
                        Intent intent = new Intent(this, DeckActivity.class);
                        intent.putExtra("deck_id", deck.id);
                        intent.putExtra("deck_title", deck.title);
                        startActivity(intent);
                    },
                    deck -> {
                        dbHelper.deleteDeck(deck.id);
                        loadDecks();

                    });
            recyclerView.setAdapter(deckAdapter);
        } else {
            deckAdapter.updateDecks(decks);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDecks();
    }

    private void showCreateDeckDialog() {
        DeckNameFragment dialog = new DeckNameFragment();
        dialog.setOnDeckNameListener(deckName -> {
            long deckId = dbHelper.saveDeck(deckName);
            if (deckId != -1) {
                Intent intent = new Intent(this, CreateDeckActivity.class);
                intent.putExtra("deck_id", deckId);
                intent.putExtra("deck_title", deckName);
                startActivity(intent);
            }
        });
        dialog.show(getSupportFragmentManager(), "deck_name_dialog");
    }
}