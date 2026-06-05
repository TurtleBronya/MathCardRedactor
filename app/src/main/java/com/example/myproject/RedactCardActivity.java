package com.example.myproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RedactCardActivity extends AppCompatActivity {

    private static final String TAG = "RedactCardActivity";

    private LinearLayout questionContainer;
    private LinearLayout answerContainer;
    private ScrollView questionScrollView;
    private ScrollView answerScrollView;
    private Button btnQuestionSide;
    private Button btnAnswerSide;
    private TextView tvCardTitle;
    private DatabaseHelper dbHelper;
    private long currentCardId = -1;
    private long currentDeckId = -1;
    private String cardTitle = "Новая карточка";
    private boolean isQuestionSide = true;
    private List<CardItem> questionItems = new ArrayList<>();
    private List<CardItem> answerItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.redact_card_activity);

        dbHelper = new DatabaseHelper(this);

        initViews();
        setupSideToggleButtons();

        currentCardId = getIntent().getLongExtra("card_id", -1);
        currentDeckId = getIntent().getLongExtra("deck_id", -1);
        cardTitle = getIntent().getStringExtra("card_title");

        if (cardTitle == null) {
            cardTitle = "Новая карточка";
        }

        tvCardTitle.setText(cardTitle);
        tvCardTitle.setOnClickListener(v -> openTitleRedactDialog());

        if (currentCardId != -1) {
            loadCardData();
        }

        showActiveSide();
    }

    private void initViews() {
        questionContainer = findViewById(R.id.questionContainer);
        answerContainer = findViewById(R.id.answerContainer);
        questionScrollView = findViewById(R.id.questionScrollView);
        answerScrollView = findViewById(R.id.answerScrollView);
        btnQuestionSide = findViewById(R.id.btnQuestionSide);
        btnAnswerSide = findViewById(R.id.btnAnswerSide);
        tvCardTitle = findViewById(R.id.etCardTitle);

        setupContainer(questionContainer);
        setupContainer(answerContainer);
    }

    private void setupContainer(LinearLayout container) {
        if (container != null) {
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(0, 8, 0, 8);
        }
    }

    private void setupSideToggleButtons() {
        btnQuestionSide.setOnClickListener(v -> {
            if (!isQuestionSide) {
                saveCurrentSideToMemory();
                isQuestionSide = true;
                showActiveSide();
                updateButtonColors();
            }
        });

        btnAnswerSide.setOnClickListener(v -> {
            if (isQuestionSide) {
                saveCurrentSideToMemory();
                isQuestionSide = false;
                showActiveSide();
                updateButtonColors();
            }
        });
    }

    private void updateButtonColors() {
        if (isQuestionSide) {
            btnQuestionSide.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue));
            btnAnswerSide.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray));
        } else {
            btnQuestionSide.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray));
            btnAnswerSide.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue));
        }
    }

    private void showActiveSide() {
        if (isQuestionSide) {
            questionScrollView.setVisibility(View.VISIBLE);
            answerScrollView.setVisibility(View.GONE);
            refreshContainerFromMemory(questionContainer, questionItems);
        } else {
            questionScrollView.setVisibility(View.GONE);
            answerScrollView.setVisibility(View.VISIBLE);
            refreshContainerFromMemory(answerContainer, answerItems);
        }
    }

    private void refreshContainerFromMemory(LinearLayout container, List<CardItem> items) {
        if (container == null) return;

        container.removeAllViews();
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 40));
        spacer.setVisibility(View.INVISIBLE);
        container.addView(spacer);

        for (CardItem item : items) {
            View view = createViewFromItem(item);
            if (view != null) {
                container.addView(view);
            }
        }
    }

    private View createViewFromItem(CardItem item) {
        if (item.type.equals("text")) {
            return createTextView(item.content);
        } else if (item.type.equals("formula")) {
            return createFormulaView(item.content);
        } else if (item.type.equals("image")) {
            return createImageView(item.content);
        }
        return null;
    }

    private void saveCurrentSideToMemory() {
        LinearLayout currentContainer = isQuestionSide ? questionContainer : answerContainer;
        List<CardItem> currentItems = isQuestionSide ? questionItems : answerItems;

        currentItems.clear();
        for (int i = 0; i < currentContainer.getChildCount(); i++) {
            View child = currentContainer.getChildAt(i);
            if (child.getVisibility() == View.INVISIBLE) continue;

            CardItem item = new CardItem();
            if (child instanceof TextView) {
                item.type = "text";
                item.content = ((TextView) child).getText().toString();
                currentItems.add(item);
            } else if (child instanceof WebView) {
                item.type = "formula";
                Object tag = child.getTag();
                item.content = (tag instanceof String) ? (String) tag : "";
                currentItems.add(item);
            } else if (child instanceof ImageView) {
                item.type = "image";
                Object tag = child.getTag();
                item.content = (tag instanceof String) ? (String) tag : "";
                currentItems.add(item);
            }
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
                cardTitle = card.title;
                tvCardTitle.setText(cardTitle);

                // Загружаем вопрос и ответ из JSON
                if (card.question != null && !card.question.isEmpty()) {
                    loadItemsFromJson(card.question, questionItems);
                }
                if (card.answer != null && !card.answer.isEmpty()) {
                    loadItemsFromJson(card.answer, answerItems);
                }
                break;
            }
        }

        refreshContainerFromMemory(questionContainer, questionItems);
        refreshContainerFromMemory(answerContainer, answerItems);
    }

    private void loadItemsFromJson(String json, List<CardItem> items) {
        items.clear();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                CardItem item = new CardItem();
                item.type = obj.getString("type");
                item.content = obj.getString("content");
                items.add(item);
            }
        } catch (JSONException e) {
            // Если не JSON, загружаем как обычный текст (для обратной совместимости)
            Log.e(TAG, "Error parsing JSON, loading as text", e);
            CardItem item = new CardItem();
            item.type = "text";
            item.content = json;
            items.add(item);
        }
    }

    private String saveItemsToJson(List<CardItem> items) {
        JSONArray jsonArray = new JSONArray();
        for (CardItem item : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("type", item.type);
                obj.put("content", item.content);
                jsonArray.put(obj);
            } catch (JSONException e) {
                Log.e(TAG, "Error saving item to JSON", e);
            }
        }
        return jsonArray.toString();
    }

    private void autoSave() {
        saveCurrentSideToMemory();

        String questionJson = saveItemsToJson(questionItems);
        String answerJson = saveItemsToJson(answerItems);

        if (currentCardId == -1) {
            currentCardId = dbHelper.saveCard(cardTitle, questionJson, answerJson);

            if (currentDeckId != -1 && currentCardId != -1) {
                dbHelper.addCardToDeck(currentDeckId, currentCardId);
            }

            Log.d(TAG, "Created new card with ID: " + currentCardId);
        } else {
            dbHelper.updateCard(currentCardId, cardTitle, questionJson, answerJson);
            Log.d(TAG, "Updated card ID: " + currentCardId);
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btntext) {
            openTextDialog();
        } else if (id == R.id.btnform) {
            openFormulaDialog();
        } else if (id == R.id.btnimg) {
            openImageDialog();
        }
    }

    private LinearLayout getCurrentContainer() {
        return isQuestionSide ? questionContainer : answerContainer;
    }

    // ─── Text ─────────────────────────────────────────────────────────────────

    private TextView createTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(20f);
        tv.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(16, 0, 16, 8);
        tv.setLayoutParams(lp);

        tv.setOnClickListener(v -> openTextRedactDialog(tv));
        return tv;
    }

    private void openTextDialog() {
        TextFragment dialog = new TextFragment();
        dialog.setOnTextAddedListener(text -> {
            TextView tv = createTextView(text);
            getCurrentContainer().addView(tv);
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
            ((LinearLayout) tv.getParent()).removeView(tv);
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
                LinearLayout.LayoutParams.MATCH_PARENT,
                300);
        lp.setMargins(16, 0, 16, 8);
        wv.setLayoutParams(lp);

        // Убираем фон и делаем прозрачным для слияния с карточкой
        wv.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        wv.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);

        WebSettings webSettings = wv.getSettings();
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        int scale = calculateScaleForLatex(latex);
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
        if (latex == null || latex.isEmpty()) return 250;
        int significantChars = countSignificantLatexChars(latex);
        if (significantChars <= 20) return 250;
        else if (significantChars <= 40) return 225;
        else if (significantChars <= 60) return 200;
        else if (significantChars <= 80) return 175;
        else if (significantChars <= 100) return 150;
        else return 125;
    }

    private int countSignificantLatexChars(String latex) {
        int count = 0;
        int i = 0;
        int length = latex.length();
        while (i < length) {
            char c = latex.charAt(i);
            if (c == '\\') {
                i++;
                while (i < length && Character.isLetter(latex.charAt(i))) i++;
                if (i < length && latex.charAt(i) == ' ') i++;
                continue;
            }
            if (Character.isWhitespace(c) || c == '{' || c == '}') {
                i++;
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == '+' || c == '-' || c == '*' || c == '/' ||
                    c == '=' || c == '<' || c == '>' || c == '^' || c == '_' || c == '[' ||
                    c == ']' || c == '(' || c == ')' || c == '|' || c == '&' || c == '!' ||
                    c == '?' || c == '~' || c == ',' || c == '.' || c == ';' || c == ':' ||
                    c == '@' || c == '#' || c == '$' || c == '%') {
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
            getCurrentContainer().addView(wv);
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
            ((LinearLayout) wv.getParent()).removeView(wv);
            autoSave();
        });
        dialog.show(getSupportFragmentManager(), "form_redact_dialog");
    }

    // ─── Image ────────────────────────────────────────────────────────────────

    private void openImageDialog() {
        ImgFragment dialog = new ImgFragment();
        dialog.setOnImageAddedListener(imageBase64 -> {
            ImageView iv = createImageView(imageBase64);
            getCurrentContainer().addView(iv);
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
                updateImageView(iv, newImageBase64);
                autoSave();
            }
            @Override
            public void onImageDeleted() {
                ((LinearLayout) iv.getParent()).removeView(iv);
                autoSave();
            }
        });
        dialog.show(getSupportFragmentManager(), "img_redact_dialog");
    }

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

    private ImageView createImageView(String imageBase64) {
        ImageView iv = new ImageView(this);
        iv.setTag(imageBase64);

        if (imageBase64 != null && !imageBase64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                iv.setImageBitmap(decodedBitmap);
                int imageWidth = decodedBitmap.getWidth();
                int imageHeight = decodedBitmap.getHeight();
                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int maxWidth = screenWidth - 64;
                int scaledHeight = (int) ((float) imageHeight * maxWidth / imageWidth);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(maxWidth, scaledHeight);
                lp.setMargins(16, 8, 16, 16);
                iv.setLayoutParams(lp);
                iv.setScaleType(ImageView.ScaleType.FIT_XY);
                iv.setAdjustViewBounds(true);
            } catch (Exception e) {
                e.printStackTrace();
                setDefaultImageParams(iv);
            }
        } else {
            setDefaultImageParams(iv);
        }

        iv.setOnClickListener(v -> openImageRedactDialog(iv, (String) iv.getTag()));
        return iv;
    }

    private void setDefaultImageParams(ImageView iv) {
        iv.setImageResource(android.R.drawable.ic_menu_gallery);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400);
        lp.setMargins(16, 8, 16, 16);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCurrentSideToMemory();
        autoSave();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveCurrentSideToMemory();
        autoSave();
    }
}