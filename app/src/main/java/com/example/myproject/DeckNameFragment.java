package com.example.myproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

public class DeckNameFragment extends DialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public interface OnDeckNameListener {
        void onDeckNameEntered(String deckName);
    }

    private OnDeckNameListener listener;

    public void setOnDeckNameListener(OnDeckNameListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_deck_name, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvDialogTitle = view.findViewById(R.id.tvDialogTitle);
        EditText etCardName = view.findViewById(R.id.etCardName);
        Button btnCreateCard = view.findViewById(R.id.btnCreateCard);

        // Меняем заголовок для создания колоды
        tvDialogTitle.setText("Создание новой колоды");
        etCardName.setHint("Введите название колоды");

        btnCreateCard.setOnClickListener(v -> {
            String deckName = etCardName.getText().toString().trim();
            if (!deckName.isEmpty() && listener != null) {
                listener.onDeckNameEntered(deckName);
            }
            dismiss();
        });
    }
}