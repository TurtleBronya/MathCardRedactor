package com.example.myproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.DialogFragment;

public class TitleRedactFragment extends DialogFragment {

    private EditText editTextContent;
    private Button btnRedact, btnCancel;
    private OnTitleChangedListener listener;
    private String currentTitle;

    public interface OnTitleChangedListener {
        void onTitleChanged(String newTitle);
    }

    public void setOnTitleChangedListener(OnTitleChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTitle = getArguments().getString("current_title", "");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_redact_title, container, false);

        editTextContent = view.findViewById(R.id.editTextContent);
        btnRedact = view.findViewById(R.id.btnRedact);
        btnCancel = view.findViewById(R.id.btnCancel);

        // Устанавливаем текущее название
        if (currentTitle != null) {
            editTextContent.setText(currentTitle);
            editTextContent.setSelection(editTextContent.getText().length());
        }

        btnRedact.setOnClickListener(v -> {
            String newTitle = editTextContent.getText().toString().trim();
            if (TextUtils.isEmpty(newTitle)) {
                newTitle = "Без названия";
            }
            if (listener != null) {
                listener.onTitleChanged(newTitle);
            }
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }
}