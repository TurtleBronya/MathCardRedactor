package com.example.myproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class RedactCardActivity extends AppCompatActivity {

    private LinearLayout editorContainer;
    private TextView tvCardTitle;
    private DatabaseHelper dbHelper;
    private long currentCardId = -1;
    private String cardTitle = "Новая карточка";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.redact_card_activity);

        dbHelper = new DatabaseHelper(this);
        editorContainer = findViewById(R.id.editorContainer);
        tvCardTitle = findViewById(R.id.etCardTitle);


        if (editorContainer instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) editorContainer;
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setPadding(0, 8, 0, 8);
            linearLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_NONE);
        }

        currentCardId = getIntent().getLongExtra("card_id", -1);
        cardTitle = getIntent().getStringExtra("card_title");
        if (cardTitle == null) {
            cardTitle = "Новая карточка";
        }

        tvCardTitle.setText(cardTitle);
        tvCardTitle.setOnClickListener(v -> openTitleRedactDialog());

        if (currentCardId != -1) {
            loadCardData();
        }
    }


    private void openTitleRedactDialog() {
        TitleRedactFragment dialog = new TitleRedactFragment();

        Bundle args = new Bundle();
        args.putString("current_title", cardTitle);
        dialog.setArguments(args);

        dialog.setOnTitleChangedListener(newTitle -> {
            cardTitle = newTitle;
            tvCardTitle.setText(cardTitle);
            autoSave();
        });

        dialog.show(getSupportFragmentManager(), "title_redact_dialog");
    }



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

    // Обновите метод addItemToContainer (если нужно добавить отступы)
    private void addItemToContainer(CardItem item) {
        if (item.type.equals("text")) {
            TextView tv = createTextView(item.content);
            editorContainer.addView(tv);
        } else if (item.type.equals("formula")) {
            WebView wv = createFormulaView(item.content);
            editorContainer.addView(wv);
        } else if (item.type.equals("image")) {
            ImageView iv = createImageView(item.content);
            editorContainer.addView(iv);
        }
    }



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
        tv.setPadding(16, 12, 16, 12);  // Уменьшил паддинги с 8 до 12 по вертикали
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 8);  // Уменьшил нижний отступ с 16 до 8dp
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

        // Вычисляем масштаб в зависимости от длины формулы
        int scale = calculateScaleForLatex(latex);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                300);
        lp.setMargins(16, 0, 16, 8);
        wv.setLayoutParams(lp);
        wv.setBackgroundResource(R.drawable.edit_bg);

        // Устанавливаем фон для WebView
        wv.setBackgroundColor(getColor(R.color.grey_white));// #F9F9F9 в ARGB формате

        WebSettings webSettings = wv.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        wv.setInitialScale(scale);

        wv.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                openFormulaRedactDialog(wv);
            }
            return true;
        });
        return wv;
    }


    private int calculateScaleForLatex(String latex) {
        if (latex == null || latex.isEmpty()) {
            return 250;
        }


        int significantChars = countSignificantLatexChars(latex);


        if (significantChars <= 20) {
            return 250; // 100%
        } else if (significantChars <= 40) {
            return 225; // 90%
        } else if (significantChars <= 60) {
            return 200; // 80%
        } else if (significantChars <= 80) {
            return 175; // 70%
        } else if (significantChars <= 100) {
            return 150; // 60%
        } else {
            return 125; // 50%
        }
    }


    private int countSignificantLatexChars(String latex) {
        int count = 0;
        int i = 0;
        int length = latex.length();

        while (i < length) {
            char c = latex.charAt(i);


            if (c == '\\') {

                i++;
                while (i < length && Character.isLetter(latex.charAt(i))) {
                    i++;
                }

                if (i < length && latex.charAt(i) == ' ') {
                    i++;
                }
                continue;
            }


            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }


            if (c == '{' || c == '}') {
                i++;
                continue;
            }


            if (Character.isLetterOrDigit(c) ||
                    c == '+' || c == '-' || c == '*' || c == '/' ||
                    c == '=' || c == '<' || c == '>' || c == '^' ||
                    c == '_' || c == '[' || c == ']' || c == '(' ||
                    c == ')' || c == '|' || c == '&' || c == '!' ||
                    c == '?' || c == '~' || c == ',' || c == '.' ||
                    c == ';' || c == ':' || c == '@' || c == '#' ||
                    c == '$' || c == '%') {
                count++;
            }

            i++;
        }

        return count;
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
    private void openImageDialog() {
        ImgFragment dialog = new ImgFragment();
        dialog.setOnImageAddedListener(imageBase64 -> {
            ImageView iv = createImageView(imageBase64);
            editorContainer.addView(iv);
            autoSave();
        });
        dialog.show(getSupportFragmentManager(), "img_dialog");
    }
    private void openImageRedactDialog(ImageView iv, String imageBase64) {
        ImgRedactFragment dialog = new ImgRedactFragment();

        Bundle args = new Bundle();
        args.putString("current_image", imageBase64);
        dialog.setArguments(args);

        dialog.setOnImageChangedListener(new ImgRedactFragment.OnImageChangedListener() {
            @Override
            public void onImageUpdated(String newImageBase64) {
                // Обновляем изображение
                updateImageView(iv, newImageBase64);
                autoSave();
            }

            @Override
            public void onImageDeleted() {
                // Удаляем изображение
                editorContainer.removeView(iv);
                autoSave();
            }
        });

        dialog.show(getSupportFragmentManager(), "img_redact_dialog");
    }

    // Обновление ImageView с новым Base64
    private void updateImageView(ImageView iv, String imageBase64) {
        try {
            byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            iv.setImageBitmap(decodedBitmap);
            iv.setTag(imageBase64);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Обновите метод createImageView
    private ImageView createImageView(String imageBase64) {
        ImageView iv = new ImageView(this);
        iv.setTag(imageBase64);

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                iv.setImageBitmap(decodedBitmap);

                // ТОЛЬКО ЗДЕСЬ подстраиваем размер под изображение
                int imageWidth = decodedBitmap.getWidth();
                int imageHeight = decodedBitmap.getHeight();
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int maxWidth = screenWidth - 64; // отступы 32dp с каждой стороны

                // Вычисляем высоту с сохранением пропорций
                int scaledHeight = (int) ((float) imageHeight * maxWidth / imageWidth);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        maxWidth,
                        scaledHeight);
                lp.setMargins(16, 8, 16, 16); // отступы для красивого отображения
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.FIT_XY);
                iv.setAdjustViewBounds(true);

            } catch (Exception e) {
                e.printStackTrace();
                // Если ошибка, устанавливаем стандартный размер
                iv.setImageResource(android.R.drawable.ic_menu_gallery);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        400);
                lp.setMargins(16, 8, 16, 16);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        } else {
            // Если нет изображения, устанавливаем заглушку со стандартным размером
            iv.setImageResource(android.R.drawable.ic_menu_gallery);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    400);
            lp.setMargins(16, 8, 16, 16);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        // Добавляем клик для редактирования
        iv.setOnClickListener(v -> openImageRedactDialog(iv, (String) iv.getTag()));

        return iv;
    }





}