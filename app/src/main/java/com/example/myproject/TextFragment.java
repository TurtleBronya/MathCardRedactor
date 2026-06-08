package com.example.myproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
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
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    public interface OnFormulaAddedListener {
        void onFormulaAdded(String text);
    }

    private OnFormulaAddedListener listener;

    public void setOnFormulaAddedListener(OnFormulaAddedListener l) {
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

        // Используем правильные ID из fragment_text.xml
        EditText editLatex = view.findViewById(R.id.editLatex);
        WebView wvPreview = view.findViewById(R.id.wvLatexPreview);
        Button btnPreview = view.findViewById(R.id.btnPreview);
        Button btnAdd = view.findViewById(R.id.btnAdd);

        // Используем inline режим для предпросмотра текста с формулами
        KaTeXWebView_inline.configure(wvPreview);
        KaTeXWebView_inline.render(wvPreview, "");

        btnPreview.setOnClickListener(v ->
                KaTeXWebView_inline.render(wvPreview, editLatex.getText().toString().trim()));

        btnAdd.setOnClickListener(v -> {
            String text = editLatex.getText().toString().trim();
            if (!text.isEmpty() && listener != null) {
                listener.onFormulaAdded(text);
            }
            dismiss();
        });
    }
}