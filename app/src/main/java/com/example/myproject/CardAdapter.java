package com.example.myproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private List<Card> cards;
    private OnCardClickListener clickListener;
    private OnCardDeleteListener deleteListener;

    public interface OnCardClickListener {
        void onCardClick(Card card);
    }

    public interface OnCardDeleteListener {
        void onCardDelete(Card card);
    }

    public CardAdapter(List<Card> cards, OnCardClickListener clickListener,
                       OnCardDeleteListener deleteListener) {
        this.cards = cards;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Card card = cards.get(position);
        holder.title.setText(card.title);

        // Показываем время последнего обновления или создания
        long timeToShow = (card.updatedAt != 0) ? card.updatedAt : card.createdAt;
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(new Date(timeToShow * 1000));

        if (card.updatedAt != 0 && card.updatedAt != card.createdAt) {
            holder.date.setText("Обновлено: " + date);
        } else {
            holder.date.setText("Создано: " + date);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onCardClick(card));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onCardDelete(card));
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    public void updateCards(List<Card> newCards) {
        this.cards = newCards;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, date;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.cardTitle);
            date = itemView.findViewById(R.id.cardDate);
            btnDelete = itemView.findViewById(R.id.btnDeleteCard);
        }
    }
}