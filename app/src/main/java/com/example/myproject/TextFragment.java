package com.example.myproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.DialogFragment;

public class TextFragment extends DialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    public interface OnTextAddedListener {
        void onTextAdded(String text);
    }

    private OnTextAddedListener listener;

    public void setOnTextAddedListener(OnTextAddedListener l) {
        this.listener = l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_text, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editText = view.findViewById(R.id.editTextContent);
        Button btnAdd    = view.findViewById(R.id.btnAdd);
        Button btnCancel = view.findViewById(R.id.btnCancel);

        btnAdd.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty() && listener != null) {
                listener.onTextAdded(text);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }
}