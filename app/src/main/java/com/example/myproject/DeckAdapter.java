package com.example.myproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {

    private List<Deck> decks;
    private final OnDeckClickListener onDeckClickListener;
    private final OnDeckDeleteListener onDeckDeleteListener;

    public interface OnDeckClickListener {
        void onDeckClick(Deck deck);
    }

    public interface OnDeckDeleteListener {
        void onDeckDelete(Deck deck);
    }

    public DeckAdapter(List<Deck> decks, OnDeckClickListener clickListener, OnDeckDeleteListener deleteListener) {
        this.decks = decks != null ? decks : new ArrayList<>();
        this.onDeckClickListener = clickListener;
        this.onDeckDeleteListener = deleteListener;
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_deck, parent, false);
        return new DeckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        Deck deck = decks.get(position);
        holder.bind(deck);

        holder.itemView.setOnClickListener(v -> {
            if (onDeckClickListener != null) {
                onDeckClickListener.onDeckClick(deck);
            }
        });

        holder.btnDeleteDeck.setOnClickListener(v -> {
            if (onDeckDeleteListener != null) {
                onDeckDeleteListener.onDeckDelete(deck);
            }
        });
    }

    @Override
    public int getItemCount() {
        return decks.size();
    }

    public void updateDecks(List<Deck> newDecks) {
        this.decks = newDecks != null ? newDecks : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class DeckViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDeckTitle;
        private final TextView tvDeckDate;
        private final TextView tvCardCount;
        private final ImageButton btnDeleteDeck;

        public DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeckTitle = itemView.findViewById(R.id.deckTitle);
            tvDeckDate = itemView.findViewById(R.id.deckDate);
            tvCardCount = itemView.findViewById(R.id.cardCount);
            btnDeleteDeck = itemView.findViewById(R.id.btnDeleteDeck);
        }

        public void bind(Deck deck) {
            tvDeckTitle.setText(deck.title);

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String date = sdf.format(new Date(deck.updatedAt * 1000));
            tvDeckDate.setText("Обновлено: " + date);

            int cardCount = deck.cards != null ? deck.cards.size() : 0;
            String cardWord = getCardWord(cardCount);
            tvCardCount.setText(cardCount + " " + cardWord);
        }

        private String getCardWord(int count) {
            if (count % 10 == 1 && count % 100 != 11) return "карточка";
            if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) return "карточки";
            return "карточек";
        }
    }
}