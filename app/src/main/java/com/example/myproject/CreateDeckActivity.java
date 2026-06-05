package com.example.myproject;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class CreateDeckActivity extends AppCompatActivity {
    private RecyclerView addedCardsRecyclerView;
    private RecyclerView availableCardsRecyclerView;
    private CardAdapter addedCardsAdapter;
    private CardAdapter availableCardsAdapter;
    private DatabaseHelper dbHelper;
    private long deckId;
    private String deckTitle;
    private TextView tvDeckTitle;
    private EditText searchBar;
    private Button btnFinish;
    private FloatingActionButton fabCreateCard;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_deck);

        dbHelper = new DatabaseHelper(this);

        deckId = getIntent().getLongExtra("deck_id", -1);
        deckTitle = getIntent().getStringExtra("deck_title");
        isEditMode = getIntent().getBooleanExtra("is_edit_mode", false);

        if (deckId == -1) {

            finish();
            return;
        }

        initViews();
        setupRecyclerViews();
        setupSearch();
        setupButtons();

        if (isEditMode) {
            btnFinish.setText("Сохранить изменения");
            tvDeckTitle.setText("Редактирование колоды: " + deckTitle);
        } else {
            btnFinish.setText("Завершить создание колоды");
            tvDeckTitle.setText(deckTitle);
        }

        loadAddedCards();
        loadAvailableCards();
    }

    private void initViews() {
        tvDeckTitle = findViewById(R.id.tvDeckTitle);
        addedCardsRecyclerView = findViewById(R.id.addedCardsRecyclerView);
        availableCardsRecyclerView = findViewById(R.id.availableCardsRecyclerView);
        searchBar = findViewById(R.id.searchBar);
        btnFinish = findViewById(R.id.btnFinish);
        fabCreateCard = findViewById(R.id.fabCreateCard);

        if (!isEditMode) {
            tvDeckTitle.setText(deckTitle);
        }
    }

    private void setupRecyclerViews() {
        addedCardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        availableCardsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadAvailableCards(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupButtons() {
        fabCreateCard.setOnClickListener(v -> showCreateCardDialog());

        btnFinish.setOnClickListener(v -> {
            if (isEditMode) {
                finish();
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadAddedCards() {
        List<Card> addedCards = dbHelper.getCardsByDeckId(deckId);

        if (addedCards.isEmpty()) {
            addedCardsRecyclerView.setVisibility(View.GONE);
        } else {
            addedCardsRecyclerView.setVisibility(View.VISIBLE);
            updateAddedCardsAdapter(addedCards);
        }
    }

    private void loadAvailableCards() {
        loadAvailableCards("");
    }

    private void loadAvailableCards(String query) {
        List<Card> availableCards = dbHelper.getAvailableCardsForDeck(deckId, query);
        updateAvailableCardsAdapter(availableCards);
    }

    private void updateAddedCardsAdapter(List<Card> cards) {
        if (addedCardsAdapter == null) {
            addedCardsAdapter = new CardAdapter(cards,
                    card -> {
                        boolean removed = dbHelper.removeCardFromDeck(deckId, card.id);
                        if (removed) {
                            loadAddedCards();
                            loadAvailableCards(searchBar.getText().toString());

                        }
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
                        dbHelper.deleteCardPermanently(card.id);
                        loadAddedCards();
                        loadAvailableCards(searchBar.getText().toString());

                    },
                    true
            );
            addedCardsRecyclerView.setAdapter(addedCardsAdapter);
        } else {
            addedCardsAdapter.updateCards(cards);
        }
    }

    private void updateAvailableCardsAdapter(List<Card> cards) {
        if (availableCardsAdapter == null) {
            availableCardsAdapter = new CardAdapter(cards,
                    card -> {
                        boolean added = dbHelper.addCardToDeck(deckId, card.id);
                        if (added) {
                            loadAddedCards();
                            loadAvailableCards(searchBar.getText().toString());

                        } else {

                        }
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
                        dbHelper.deleteCardPermanently(card.id);
                        loadAddedCards();
                        loadAvailableCards(searchBar.getText().toString());

                    },
                    false
            );
            availableCardsRecyclerView.setAdapter(availableCardsAdapter);
        } else {
            availableCardsAdapter.updateCards(cards);
        }
    }

    private void showCreateCardDialog() {
        CardNameFragment dialog = new CardNameFragment();
        dialog.setOnCardNameListener(cardName -> {
            Intent intent = new Intent(this, RedactCardActivity.class);
            intent.putExtra("deck_id", deckId);
            intent.putExtra("card_title", cardName);
            startActivity(intent);
        });
        dialog.show(getSupportFragmentManager(), "card_name_dialog");
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAddedCards();
        loadAvailableCards(searchBar.getText().toString());
    }
}