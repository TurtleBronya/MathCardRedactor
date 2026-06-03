package com.example.myproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;

public class TextRedactFragment extends DialogFragment {
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
    public interface OnTextChangedListener {
        void onTextChanged(String text);
    }

    public interface OnDeleteListener {
        void onDelete();
    }

    private OnTextChangedListener changeListener;
    private OnDeleteListener deleteListener;

    public void setOnTextChangedListener(OnTextChangedListener l) {
        this.changeListener = l;
    }

    public void setOnDeleteListener(OnDeleteListener l) {
        this.deleteListener = l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_redact_text, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editText = view.findViewById(R.id.editTextContent);
        Button btnDelete  = view.findViewById(R.id.btnDelete);
        Button btnRedact  = view.findViewById(R.id.btnRedact);
        Button btnCancel  = view.findViewById(R.id.btnCancel);


        Bundle args = getArguments();
        if (args != null) {
            String initialText = args.getString("initial_text", "");
            editText.setText(initialText);
            editText.setSelection(initialText.length());
        }

        btnRedact.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty() && changeListener != null) {
                changeListener.onTextChanged(text);
            }
            dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete();
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }
}