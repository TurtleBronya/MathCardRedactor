package com.example.myproject;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class RedactCardActivity extends AppCompatActivity {

    private LinearLayout editorContainer;
    private EditText etCardTitle;
    private DatabaseHelper dbHelper;
    private long currentCardId = -1;
    private String cardTitle = "Новая карточка";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.redact_card_activity);

        dbHelper = new DatabaseHelper(this);
        editorContainer = findViewById(R.id.editorContainer);
        etCardTitle = findViewById(R.id.etCardTitle);

        // Получаем ID карточки если редактируем
        currentCardId = getIntent().getLongExtra("card_id", -1);
        cardTitle = getIntent().getStringExtra("card_title");
        if (cardTitle == null) {
            cardTitle = "Новая карточка";
        }

        // Устанавливаем название и слушаем его изменения
        etCardTitle.setText(cardTitle);
        etCardTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                cardTitle = s.toString().trim();
                if (cardTitle.isEmpty()) cardTitle = "Без названия";
                autoSave();
            }
        });

        if (currentCardId != -1) {
            loadCardData();
        }
    }

    // ─── Загрузка данных ──────────────────────────────────────────────────────

    private void loadCardData() {
        List<Card> cards = dbHelper.getAllCards();
        for (Card card : cards) {
            if (card.id == currentCardId) {
                for (CardItem item : card.items) {
                    addItemToContainer(item);
                }
                break;
            }
        }
    }

    private void addItemToContainer(CardItem item) {
        if (item.type.equals("text")) {
            TextView tv = createTextView(item.content);
            editorContainer.addView(tv);

        } else if (item.type.equals("formula")) {
            WebView wv = createFormulaView(item.content);
            editorContainer.addView(wv);

        } else if (item.type.equals("image")) {
            ImageView iv = createImageView(Uri.parse(item.content), item.content);
            editorContainer.addView(iv);
        }
    }

    // ─── Автосохранение ───────────────────────────────────────────────────────

    /**
     * Собирает текущее состояние редактора и сохраняет/обновляет карточку в БД.
     * Если карточка ещё не создана — создаёт её и запоминает ID.
     */
    private void autoSave() {
        List<CardItem> items = collectItems();

        if (currentCardId == -1) {
            currentCardId = dbHelper.saveCard(cardTitle, items);
        } else {
            dbHelper.updateCard(currentCardId, cardTitle, items);
        }
    }

    private List<CardItem> collectItems() {
        List<CardItem> items = new ArrayList<>();
        for (int i = 0; i < editorContainer.getChildCount(); i++) {
            View child = editorContainer.getChildAt(i);
            CardItem item = new CardItem();

            if (child instanceof TextView) {
                item.type = "text";
                item.content = ((TextView) child).getText().toString();
            } else if (child instanceof WebView) {
                item.type = "formula";
                item.content = (String) child.getTag();
            } else if (child instanceof ImageView) {
                item.type = "image";
                // URI сохранён в теге при создании ImageView
                Object tag = child.getTag();
                item.content = (tag instanceof String) ? (String) tag : "";
            } else {
                continue; // пропускаем служебные View, если есть
            }

            items.add(item);
        }
        return items;
    }

    // ─── Обработка кнопок панели ──────────────────────────────────────────────

    public void onClick(View v) {
        if (v.getId() == R.id.btntext) {
            openTextDialog();
        } else if (v.getId() == R.id.btnform) {
            openFormulaDialog();
        } else if (v.getId() == R.id.btnimg) {
            openImageDialog();
        }
    }

    // ─── Text ─────────────────────────────────────────────────────────────────

    private TextView createTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(16f);
        tv.setPadding(8, 8, 8, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        tv.setLayoutParams(lp);
        tv.setOnClickListener(v -> openTextRedactDialog(tv));
        return tv;
    }

    private void openTextDialog() {
        TextFragment dialog = new TextFragment();
        dialog.setOnTextAddedListener(text -> {
            TextView tv = createTextView(text);
            editorContainer.addView(tv);
            autoSave(); // ← сохраняем после добавления
        });
        dialog.show(getSupportFragmentManager(), "text_dialog");
    }

    private void openTextRedactDialog(TextView tv) {
        TextRedactFragment dialog = new TextRedactFragment();

        Bundle args = new Bundle();
        args.putString("initial_text", tv.getText().toString());
        dialog.setArguments(args);

        dialog.setOnTextChangedListener(newText -> {
            tv.setText(newText);
            autoSave(); // ← сохраняем после изменения
        });
        dialog.setOnDeleteListener(() -> {
            editorContainer.removeView(tv);
            autoSave(); // ← сохраняем после удаления
        });

        dialog.show(getSupportFragmentManager(), "text_redact_dialog");
    }

    // ─── Formula ──────────────────────────────────────────────────────────────

    private WebView createFormulaView(String latex) {
        WebView wv = KaTeXWebView.create(this);
        KaTeXWebView.render(wv, latex);
        wv.setTag(latex);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 200);
        lp.setMargins(0, 0, 0, 16);
        wv.setLayoutParams(lp);
        wv.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                openFormulaRedactDialog(wv);
            }
            return true;
        });
        return wv;
    }

    private void openFormulaDialog() {
        FormFragment dialog = new FormFragment();
        dialog.setOnFormulaAddedListener(latex -> {
            WebView wv = createFormulaView(latex);
            editorContainer.addView(wv);
            autoSave(); // ← сохраняем после добавления
        });
        dialog.show(getSupportFragmentManager(), "form_dialog");
    }

    private void openFormulaRedactDialog(WebView wv) {
        FormRedactFragment dialog = new FormRedactFragment();
        Bundle args = new Bundle();
        String currentLatex = wv.getTag() instanceof String ? (String) wv.getTag() : "";
        args.putString("initial_form", currentLatex);
        dialog.setArguments(args);

        dialog.setOnFormulaAddedListener(latex -> {
            wv.setTag(latex);
            KaTeXWebView.render(wv, latex);
            autoSave(); // ← сохраняем после изменения
        });
        dialog.setOnDeleteListener(() -> {
            editorContainer.removeView(wv);
            autoSave(); // ← сохраняем после удаления
        });

        dialog.show(getSupportFragmentManager(), "form_redact_dialog");
    }

    // ─── Image ────────────────────────────────────────────────────────────────

    private ImageView createImageView(Uri uri, String uriString) {
        ImageView iv = new ImageView(this);
        iv.setImageURI(uri);
        iv.setTag(uriString); // сохраняем URI строкой для последующего сохранения в БД
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400);
        lp.setMargins(0, 0, 0, 16);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return iv;
    }

    private void openImageDialog() {
        ImgFragment dialog = new ImgFragment();
        dialog.setOnImageAddedListener(uri -> {
            ImageView iv = createImageView(uri, uri.toString());
            editorContainer.addView(iv);
            autoSave(); // ← сохраняем после добавления
        });
        dialog.show(getSupportFragmentManager(), "img_dialog");
    }
}