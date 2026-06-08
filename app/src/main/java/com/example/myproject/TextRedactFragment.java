package com.example.myproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
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
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public interface OnFormulaAddedListener {
        void onFormulaAdded(String text);
    }

    public interface OnDeleteListener {
        void onDelete();
    }

    private OnFormulaAddedListener listener;
    private OnDeleteListener deleteListener;

    public void setOnFormulaAddedListener(OnFormulaAddedListener l) {
        this.listener = l;
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

        // Используем правильные ID из fragment_redact_text.xml
        EditText editLatex = view.findViewById(R.id.editLatex);
        WebView wvPreview = view.findViewById(R.id.wvLatexPreview);
        Button btnPreview = view.findViewById(R.id.btnPreview);
        Button btnDelete = view.findViewById(R.id.btnDelete);
        Button btnRedact = view.findViewById(R.id.btnRedact);

        Bundle args = getArguments();
        if (args != null) {
            String initialForm = args.getString("initial_form", "");
            editLatex.setText(initialForm);
            editLatex.setSelection(initialForm.length());
        }

        // Используем inline режим для предпросмотра
        KaTeXWebView_inline.configure(wvPreview);
        KaTeXWebView_inline.render(wvPreview, editLatex.getText().toString().trim());

        btnPreview.setOnClickListener(v ->
                KaTeXWebView_inline.render(wvPreview, editLatex.getText().toString().trim()));

        btnRedact.setOnClickListener(v -> {
            String text = editLatex.getText().toString().trim();
            if (!text.isEmpty() && listener != null) {
                listener.onFormulaAdded(text);
            }
            dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete();
            }
            dismiss();
        });
    }
}