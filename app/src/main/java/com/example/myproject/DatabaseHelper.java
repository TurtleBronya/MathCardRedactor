package com.example.myproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cards.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_CARDS   = "cards";
    public static final String COLUMN_ID         = "_id";
    public static final String COLUMN_TITLE      = "title";
    public static final String COLUMN_CREATED_AT = "created_at";

    public static final String TABLE_ITEMS    = "items";
    public static final String COLUMN_CARD_ID = "card_id";
    public static final String COLUMN_TYPE    = "type";    // "text", "formula", "image"
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_ORDER   = "order_index";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CARDS + " (" +
                COLUMN_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE      + " TEXT, " +
                COLUMN_CREATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')))");

        db.execSQL("CREATE TABLE " + TABLE_ITEMS + " (" +
                COLUMN_ID      + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CARD_ID + " INTEGER, " +
                COLUMN_TYPE    + " TEXT, " +
                COLUMN_CONTENT + " TEXT, " +
                COLUMN_ORDER   + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_CARD_ID + ") REFERENCES " +
                TABLE_CARDS + "(" + COLUMN_ID + ") ON DELETE CASCADE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARDS);
        onCreate(db);
    }

    // ─── Сохранить новую карточку ─────────────────────────────────────────────

    public long saveCard(String title, List<CardItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_TITLE, title);
            long cardId = db.insert(TABLE_CARDS, null, cv);

            insertItems(db, cardId, items);

            db.setTransactionSuccessful();
            return cardId;
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // ─── Загрузить все карточки ───────────────────────────────────────────────

    public List<Card> getAllCards() {
        List<Card> cards = new ArrayList<>();
        // Используем одно соединение и для карточек, и для элементов
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_CARDS + " ORDER BY " + COLUMN_CREATED_AT + " DESC",
                    null);
            while (cursor.moveToNext()) {
                Card card = new Card();
                card.id        = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                card.title     = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                card.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
                card.items     = getCardItems(db, card.id); // передаём уже открытое соединение
                cards.add(card);
            }
            cursor.close();
        } finally {
            db.close();
        }
        return cards;
    }

    // ─── Загрузить элементы карточки (внутренний, принимает db) ──────────────

    private List<CardItem> getCardItems(SQLiteDatabase db, long cardId) {
        List<CardItem> items = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_ITEMS +
                        " WHERE " + COLUMN_CARD_ID + " = ?" +
                        " ORDER BY " + COLUMN_ORDER,
                new String[]{String.valueOf(cardId)});
        while (cursor.moveToNext()) {
            CardItem item = new CardItem();
            item.type    = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
            item.content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
            items.add(item);
        }
        cursor.close();
        return items;
    }

    // ─── Обновить карточку ────────────────────────────────────────────────────

    public void updateCard(long cardId, String title, List<CardItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_TITLE, title);
            db.update(TABLE_CARDS, cv, COLUMN_ID + " = ?",
                    new String[]{String.valueOf(cardId)});

            db.delete(TABLE_ITEMS, COLUMN_CARD_ID + " = ?",
                    new String[]{String.valueOf(cardId)});

            insertItems(db, cardId, items);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // ─── Удалить карточку ─────────────────────────────────────────────────────

    public void deleteCard(long cardId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_CARDS, COLUMN_ID + " = ?", new String[]{String.valueOf(cardId)});
        } finally {
            db.close();
        }
    }

    // ─── Вспомогательный метод вставки элементов ──────────────────────────────

    private void insertItems(SQLiteDatabase db, long cardId, List<CardItem> items) {
        for (int i = 0; i < items.size(); i++) {
            CardItem item = items.get(i);
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_CARD_ID, cardId);
            cv.put(COLUMN_TYPE,    item.type);
            cv.put(COLUMN_CONTENT, item.content);
            cv.put(COLUMN_ORDER,   i);
            db.insert(TABLE_ITEMS, null, cv);
        }
    }
}