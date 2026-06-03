package com.example.myproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private DatabaseHelper dbHelper;
    private EditText searchBar;
    private ImageButton btnSearchCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchBar = findViewById(R.id.Searchbar);


        FloatingActionButton fab = findViewById(R.id.fabCreate);
        fab.setOnClickListener(v -> showCreateCardDialog());

        setupSearch();

        loadCards();
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

    private void performSearch(String query) {
        List<Card> cards = dbHelper.searchCards(query);
        updateAdapter(cards);
    }

    private void loadCards() {
        performSearch("");
    }

    private void updateAdapter(List<Card> cards) {
        if (cardAdapter == null) {
            cardAdapter = new CardAdapter(cards, card -> {
                Intent intent = new Intent(this, RedactCardActivity.class);
                intent.putExtra("card_id", card.id);
                intent.putExtra("card_title", card.title);
                startActivity(intent);
            }, card -> {
                dbHelper.deleteCard(card.id);
                loadCards();
            });
            recyclerView.setAdapter(cardAdapter);
        } else {
            cardAdapter.updateCards(cards);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCards();
    }

    private void showCreateCardDialog() {
        CardNameFragment dialog = new CardNameFragment();
        dialog.setOnCardNameListener(cardName -> {
            Intent intent = new Intent(this, RedactCardActivity.class);
            intent.putExtra("card_title", cardName);
            startActivity(intent);
        });
        dialog.show(getSupportFragmentManager(), "card_name_dialog");
    }
}