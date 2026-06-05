package com.example.myproject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadCardActivity extends AppCompatActivity {

    private LinearLayout questionContainer;
    private LinearLayout answerContainer;
    private ScrollView questionScrollView;
    private ScrollView answerScrollView;
    private Button btnNextCard;
    private Button btnToggleSide;
    private TextView tvCardTitle;
    private TextView tvProgress;

    private DatabaseHelper dbHelper;
    private long deckId;
    private List<Card> cards;
    private List<CardItem> currentQuestionItems = new ArrayList<>();
    private List<CardItem> currentAnswerItems = new ArrayList<>();
    private int currentCardIndex = 0;
    private boolean isQuestionSide = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_card);

        dbHelper = new DatabaseHelper(this);

        deckId = getIntent().getLongExtra("deck_id", -1);

        if (deckId == -1) {

            finish();
            return;
        }

        initViews();
        setupClickListeners();
        loadCards();

        btnNextCard.setOnClickListener(v -> nextCard());
    }

    private void initViews() {
        questionContainer = findViewById(R.id.questionContainer);
        answerContainer = findViewById(R.id.answerContainer);
        questionScrollView = findViewById(R.id.questionScrollView);
        answerScrollView = findViewById(R.id.answerScrollView);
        btnNextCard = findViewById(R.id.btnNextCard);
        btnToggleSide = findViewById(R.id.btnToggleSide);
        tvCardTitle = findViewById(R.id.tvCardTitle);
        tvProgress = findViewById(R.id.tvProgress);

        setupContainer(questionContainer);
        setupContainer(answerContainer);
    }

    private void setupContainer(LinearLayout container) {
        if (container != null) {
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(0, 8, 0, 8);
        }
    }

    private void setupClickListeners() {
        btnToggleSide.setOnClickListener(v -> toggleCardSide());
    }

    private void toggleCardSide() {
        isQuestionSide = !isQuestionSide;
        showActiveSide();
        updateToggleButton();
    }

    private void updateToggleButton() {
        if (isQuestionSide) {
            btnToggleSide.setText("Вопрос");
            btnToggleSide.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue));
        } else {
            btnToggleSide.setText("Ответ");
            btnToggleSide.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.green));
        }
    }

    private void showActiveSide() {
        if (isQuestionSide) {
            questionScrollView.setVisibility(View.VISIBLE);
            answerScrollView.setVisibility(View.GONE);
        } else {
            questionScrollView.setVisibility(View.GONE);
            answerScrollView.setVisibility(View.VISIBLE);
        }
    }

    private void loadCards() {
        cards = dbHelper.getCardsByDeckId(deckId);

        if (cards.isEmpty()) {

            finish();
            return;
        }

        Collections.shuffle(cards);

        currentCardIndex = 0;
        loadCurrentCard();
    }

    private void loadCurrentCard() {
        if (currentCardIndex >= cards.size()) {

            finish();
            return;
        }

        Card card = cards.get(currentCardIndex);
        tvCardTitle.setText(card.title);
        tvProgress.setText((currentCardIndex + 1) + " / " + cards.size());

        loadItemsFromJson(card.question, currentQuestionItems);
        loadItemsFromJson(card.answer, currentAnswerItems);

        refreshContainer(questionContainer, currentQuestionItems);
        refreshContainer(answerContainer, currentAnswerItems);

        isQuestionSide = true;
        showActiveSide();
        updateToggleButton();
    }

    private void loadItemsFromJson(String json, List<CardItem> items) {
        items.clear();
        if (json == null || json.isEmpty()) {
            return;
        }

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
            CardItem item = new CardItem();
            item.type = "text";
            item.content = json;
            items.add(item);
        }
    }

    private void refreshContainer(LinearLayout container, List<CardItem> items) {
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

        return tv;
    }

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

                iv.setPadding(16, 16, 16, 16);

            } catch (Exception e) {
                e.printStackTrace();
                setDefaultImageParams(iv);
            }
        } else {
            setDefaultImageParams(iv);
        }

        return iv;
    }

    private void setDefaultImageParams(ImageView iv) {
        iv.setImageResource(android.R.drawable.ic_menu_gallery);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 400);
        lp.setMargins(16, 8, 16, 16);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackgroundResource(R.drawable.edit_bg);
        iv.setPadding(16, 16, 16, 16);
    }

    private void nextCard() {
        currentCardIndex++;
        if (currentCardIndex < cards.size()) {
            loadCurrentCard();
        } else {

            finish();
        }
    }
}