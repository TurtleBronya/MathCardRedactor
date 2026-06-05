package com.example.myproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.DialogFragment;

public class FormFragment extends DialogFragment {

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
        void onFormulaAdded(String latex);
    }

    private OnFormulaAddedListener listener;

    public void setOnFormulaAddedListener(OnFormulaAddedListener l) {
        this.listener = l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_form, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText editLatex  = view.findViewById(R.id.editLatex);
        WebView  wvPreview  = view.findViewById(R.id.wvLatexPreview);
        Button   btnPreview = view.findViewById(R.id.btnPreview);
        Button   btnAdd     = view.findViewById(R.id.btnAdd);


        KaTeXWebView.configure(wvPreview);
        KaTeXWebView.render(wvPreview, "");

        btnPreview.setOnClickListener(v ->
                KaTeXWebView.render(wvPreview, editLatex.getText().toString().trim()));

        btnAdd.setOnClickListener(v -> {
            String latex = editLatex.getText().toString().trim();
            if (!latex.isEmpty() && listener != null) {
                listener.onFormulaAdded(latex);
            }
            dismiss();
        });


    }
}
