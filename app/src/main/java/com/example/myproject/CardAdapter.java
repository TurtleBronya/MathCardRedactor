package com.example.myproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private List<Card> cards;
    private final OnCardClickListener onCardClickListener;
    private final OnCardEditListener onCardEditListener;
    private final OnCardDeleteListener onCardDeleteListener;
    private boolean highlightGreen = false;

    public interface OnCardClickListener {
        void onCardClick(Card card);
    }

    public interface OnCardEditListener {
        void onCardEdit(Card card);
    }

    public interface OnCardDeleteListener {
        void onCardDelete(Card card);
    }

    public CardAdapter(List<Card> cards,
                       OnCardClickListener clickListener,
                       OnCardEditListener editListener,
                       OnCardDeleteListener deleteListener) {
        this(cards, clickListener, editListener, deleteListener, false);
    }

    public CardAdapter(List<Card> cards,
                       OnCardClickListener clickListener,
                       OnCardEditListener editListener,
                       OnCardDeleteListener deleteListener,
                       boolean highlightGreen) {
        this.cards = cards != null ? cards : new ArrayList<>();
        this.onCardClickListener = clickListener;
        this.onCardEditListener = editListener;
        this.onCardDeleteListener = deleteListener;
        this.highlightGreen = highlightGreen;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.bind(card, highlightGreen);

        holder.itemView.setOnClickListener(v -> {
            if (onCardClickListener != null) {
                onCardClickListener.onCardClick(card);
            }
        });

        holder.btnEditCard.setOnClickListener(v -> {
            if (onCardEditListener != null) {
                onCardEditListener.onCardEdit(card);
            }
        });

        holder.btnDeleteCard.setOnClickListener(v -> {
            if (onCardDeleteListener != null) {
                onCardDeleteListener.onCardDelete(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public void updateCards(List<Card> newCards) {
        this.cards = newCards != null ? newCards : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCardTitle;
        private final TextView tvCardDate;
        private final ImageButton btnEditCard;
        private final ImageButton btnDeleteCard;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCardTitle = itemView.findViewById(R.id.cardTitle);
            tvCardDate = itemView.findViewById(R.id.cardDate);
            btnEditCard = itemView.findViewById(R.id.btnEditCard);
            btnDeleteCard = itemView.findViewById(R.id.btnDeleteCard);
        }

        public void bind(Card card, boolean highlightGreen) {
            tvCardTitle.setText(card.title);

            if (highlightGreen) {
                tvCardTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.green));
            } else {
                tvCardTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String date = sdf.format(new Date(card.updatedAt * 1000));
            tvCardDate.setText("Обновлено: " + date);
        }
    }
}