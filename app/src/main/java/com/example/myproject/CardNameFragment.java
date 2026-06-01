package com.example.myproject;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class CardNameFragment extends DialogFragment {

    private EditText etCardName;
    private Button btnCreate, btnCancel;
    private TextView tvTitle;
    private OnCardNameListener listener;

    public interface OnCardNameListener {
        void onCardNameEntered(String cardName);
    }

    public void setOnCardNameListener(OnCardNameListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // Инфлейтим кастомный layout для диалога
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_card_name, null);

        initViews(view);
        setupListeners();

        builder.setView(view);
        return builder.create();
    }

    private void initViews(View view) {
        etCardName = view.findViewById(R.id.etCardName);
        btnCreate = view.findViewById(R.id.btnCreateCard);
        btnCancel = view.findViewById(R.id.btnCancelCard);
        tvTitle = view.findViewById(R.id.tvDialogTitle);
    }

    private void setupListeners() {
        btnCreate.setOnClickListener(v -> {
            String cardName = etCardName.getText().toString().trim();
            if (!TextUtils.isEmpty(cardName)) {
                if (listener != null) {
                    listener.onCardNameEntered(cardName);
                }
                dismiss();
            } else {
                etCardName.setError("Введите название карточки");
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        // Настраиваем ширину диалога
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}