package com.example.myproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cards.db";
    private static final int DATABASE_VERSION = 3; // Увеличиваем версию до 3

    public static final String TABLE_CARDS   = "cards";
    public static final String COLUMN_ID         = "_id";
    public static final String COLUMN_TITLE      = "title";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_UPDATED_AT = "updated_at";

    public static final String TABLE_ITEMS    = "items";
    public static final String COLUMN_CARD_ID = "card_id";
    public static final String COLUMN_TYPE    = "type";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_ORDER   = "order_index";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("DatabaseHelper", "Creating new database");
        createTables(db);
    }

    private void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CARDS + " (" +
                COLUMN_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE      + " TEXT, " +
                COLUMN_CREATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')), " +
                COLUMN_UPDATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')))");

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
        Log.d("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 2) {
            // Добавляем колонку updated_at
            try {
                db.execSQL("ALTER TABLE " + TABLE_CARDS + " ADD COLUMN " + COLUMN_UPDATED_AT + " INTEGER DEFAULT 0");
                // Заполняем updated_at значениями из created_at для существующих записей
                db.execSQL("UPDATE " + TABLE_CARDS + " SET " + COLUMN_UPDATED_AT + " = " + COLUMN_CREATED_AT);
                Log.d("DatabaseHelper", "Added updated_at column");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error adding updated_at column", e);
            }
        }

        if (oldVersion < 3) {
            // Здесь можно добавить другие изменения для версии 3
            Log.d("DatabaseHelper", "Upgrade to version 3 completed");
        }
    }

    // ─── Сохранить новую карточку ─────────────────────────────────────────────

    public long saveCard(String title, List<CardItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_TITLE, title);
            long currentTime = System.currentTimeMillis() / 1000;
            cv.put(COLUMN_UPDATED_AT, currentTime);
            long cardId = db.insert(TABLE_CARDS, null, cv);

            insertItems(db, cardId, items);

            db.setTransactionSuccessful();
            return cardId;
        } finally {
            db.endTransaction();
        }
    }

    // ─── Загрузить все карточки ───────────────────────────────────────────────

    public List<Card> getAllCards() {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Проверяем существует ли колонка updated_at
            boolean hasUpdatedAt = false;
            try {
                Cursor testCursor = db.rawQuery("PRAGMA table_info(" + TABLE_CARDS + ")", null);
                while (testCursor.moveToNext()) {
                    String columnName = testCursor.getString(testCursor.getColumnIndex("name"));
                    if (COLUMN_UPDATED_AT.equals(columnName)) {
                        hasUpdatedAt = true;
                        break;
                    }
                }
                testCursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            String orderColumn = hasUpdatedAt ? COLUMN_UPDATED_AT : COLUMN_CREATED_AT;
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_CARDS + " ORDER BY " + orderColumn + " DESC",
                    null);

            while (cursor.moveToNext()) {
                Card card = new Card();
                card.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                card.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                card.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));

                // Получаем updated_at, если колонка существует
                try {
                    int updatedIndex = cursor.getColumnIndex(COLUMN_UPDATED_AT);
                    card.updatedAt = (updatedIndex != -1) ?
                            cursor.getLong(updatedIndex) : card.createdAt;
                } catch (Exception e) {
                    card.updatedAt = card.createdAt;
                }

                card.items = getCardItems(db, card.id);
                cards.add(card);
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
        return cards;
    }

    // ─── Загрузить элементы карточки ─────────────────────────────────────────

    private List<CardItem> getCardItems(SQLiteDatabase db, long cardId) {
        List<CardItem> items = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_ITEMS +
                            " WHERE " + COLUMN_CARD_ID + " = ?" +
                            " ORDER BY " + COLUMN_ORDER,
                    new String[]{String.valueOf(cardId)});
            while (cursor.moveToNext()) {
                CardItem item = new CardItem();
                item.type = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE));
                item.content = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT));
                items.add(item);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return items;
    }

    // ─── Обновить карточку ────────────────────────────────────────────────────

    public void updateCard(long cardId, String title, List<CardItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_TITLE, title);
            cv.put(COLUMN_UPDATED_AT, System.currentTimeMillis() / 1000);
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
            cv.put(COLUMN_TYPE, item.type);
            cv.put(COLUMN_CONTENT, item.content);
            cv.put(COLUMN_ORDER, i);
            db.insert(TABLE_ITEMS, null, cv);
        }
    }
}