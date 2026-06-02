package com.example.myproject;

import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class RedactCardActivity extends AppCompatActivity {

    private LinearLayout editorContainer;
    private TextView tvCardTitle; // Изменено с EditText на TextView
    private DatabaseHelper dbHelper;
    private long currentCardId = -1;
    private String cardTitle = "Новая карточка";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.redact_card_activity);

        dbHelper = new DatabaseHelper(this);
        editorContainer = findViewById(R.id.editorContainer);
        tvCardTitle = findViewById(R.id.etCardTitle); // Теперь это TextView

        // Получаем ID карточки если редактируем
        currentCardId = getIntent().getLongExtra("card_id", -1);
        cardTitle = getIntent().getStringExtra("card_title");
        if (cardTitle == null) {
            cardTitle = "Новая карточка";
        }

        // Устанавливаем название
        tvCardTitle.setText(cardTitle);

        // Обработчик нажатия на название для редактирования
        tvCardTitle.setOnClickListener(v -> openTitleRedactDialog());

        if (currentCardId != -1) {
            loadCardData();
        }
    }

    // Открыть диалог редактирования названия
    private void openTitleRedactDialog() {
        TitleRedactFragment dialog = new TitleRedactFragment();

        Bundle args = new Bundle();
        args.putString("current_title", cardTitle);
        dialog.setArguments(args);

        dialog.setOnTitleChangedListener(newTitle -> {
            cardTitle = newTitle;
            tvCardTitle.setText(cardTitle);
            autoSave(); // Сохраняем изменения
        });

        dialog.show(getSupportFragmentManager(), "title_redact_dialog");
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
                Object tag = child.getTag();
                item.content = (tag instanceof String) ? (String) tag : "";
            } else {
                continue;
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
            autoSave();
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
            autoSave();
        });
        dialog.setOnDeleteListener(() -> {
            editorContainer.removeView(tv);
            autoSave();
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
            autoSave();
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
            autoSave();
        });
        dialog.setOnDeleteListener(() -> {
            editorContainer.removeView(wv);
            autoSave();
        });

        dialog.show(getSupportFragmentManager(), "form_redact_dialog");
    }

    // ─── Image ────────────────────────────────────────────────────────────────

    private ImageView createImageView(Uri uri, String uriString) {
        ImageView iv = new ImageView(this);
        iv.setImageURI(uri);
        iv.setTag(uriString);
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
            autoSave();
        });
        dialog.show(getSupportFragmentManager(), "img_dialog");
    }
}