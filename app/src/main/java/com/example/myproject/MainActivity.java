package com.example.myproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private CardAdapter cardAdapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fab = findViewById(R.id.fabCreate);
        fab.setOnClickListener(v -> showCreateCardDialog());

        loadCards();
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadCards();
    }
    private void loadCards() {
        List<Card> cards = dbHelper.getAllCards();
        if (cardAdapter == null) {
            cardAdapter = new CardAdapter(cards, card -> {
                // Открыть карточку для редактирования
                Intent intent = new Intent(this, RedactCardActivity.class);
                intent.putExtra("card_id", card.id);
                intent.putExtra("card_title", card.title);
                startActivity(intent);
            }, card -> {
                // Удалить карточку
                dbHelper.deleteCard(card.id);
                loadCards();
            });
            recyclerView.setAdapter(cardAdapter);
        } else {
            cardAdapter.updateCards(cards);
        }
    }
    private void createNewCard() {
        Intent intent = new Intent(this, RedactCardActivity.class);
        startActivity(intent);
    }
    private void showCreateCardDialog() {
        CardNameFragment dialog = new CardNameFragment();
        dialog.setOnCardNameListener(cardName -> {
            // Передаём название в RedactCardActivity
            Intent intent = new Intent(this, RedactCardActivity.class);
            intent.putExtra("card_title", cardName);
            startActivity(intent);
        });
        dialog.show(getSupportFragmentManager(), "card_name_dialog");
    }
}