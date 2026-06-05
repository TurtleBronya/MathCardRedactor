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
    private static final int DATABASE_VERSION = 6;

    // Таблица колод
    public static final String TABLE_DECKS = "decks";
    public static final String COLUMN_DECK_ID = "_id";
    public static final String COLUMN_DECK_TITLE = "title";
    public static final String COLUMN_DECK_CREATED_AT = "created_at";
    public static final String COLUMN_DECK_UPDATED_AT = "updated_at";

    // Таблица карточек (общая)
    public static final String TABLE_CARDS = "cards";
    public static final String COLUMN_CARD_ID = "_id";
    public static final String COLUMN_CARD_TITLE = "title";
    public static final String COLUMN_CARD_QUESTION = "question";
    public static final String COLUMN_CARD_ANSWER = "answer";
    public static final String COLUMN_CARD_CREATED_AT = "created_at";
    public static final String COLUMN_CARD_UPDATED_AT = "updated_at";

    // Таблица связей карточек с колодами (многие ко многим)
    public static final String TABLE_DECK_CARDS = "deck_cards";
    public static final String COLUMN_DECK_CARDS_ID = "_id";
    public static final String COLUMN_DECK_REF_ID = "deck_id";
    public static final String COLUMN_CARD_REF_ID = "card_id";
    public static final String COLUMN_ADDED_AT = "added_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("DatabaseHelper", "Creating new database with shared cards");

        db.execSQL("CREATE TABLE " + TABLE_DECKS + " (" +
                COLUMN_DECK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DECK_TITLE + " TEXT NOT NULL, " +
                COLUMN_DECK_CREATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')), " +
                COLUMN_DECK_UPDATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')))");

        db.execSQL("CREATE TABLE " + TABLE_CARDS + " (" +
                COLUMN_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_CARD_TITLE + " TEXT, " +
                COLUMN_CARD_QUESTION + " TEXT, " +
                COLUMN_CARD_ANSWER + " TEXT, " +
                COLUMN_CARD_CREATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')), " +
                COLUMN_CARD_UPDATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')))");

        db.execSQL("CREATE TABLE " + TABLE_DECK_CARDS + " (" +
                COLUMN_DECK_CARDS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DECK_REF_ID + " INTEGER, " +
                COLUMN_CARD_REF_ID + " INTEGER, " +
                COLUMN_ADDED_AT + " INTEGER DEFAULT (strftime('%s', 'now')), " +
                "FOREIGN KEY(" + COLUMN_DECK_REF_ID + ") REFERENCES " + TABLE_DECKS + "(" + COLUMN_DECK_ID + ") ON DELETE CASCADE, " +
                "FOREIGN KEY(" + COLUMN_CARD_REF_ID + ") REFERENCES " + TABLE_CARDS + "(" + COLUMN_CARD_ID + ") ON DELETE CASCADE, " +
                "UNIQUE(" + COLUMN_DECK_REF_ID + ", " + COLUMN_CARD_REF_ID + "))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < 6) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CARDS + " (" +
                    COLUMN_CARD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CARD_TITLE + " TEXT, " +
                    COLUMN_CARD_QUESTION + " TEXT, " +
                    COLUMN_CARD_ANSWER + " TEXT, " +
                    COLUMN_CARD_CREATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')), " +
                    COLUMN_CARD_UPDATED_AT + " INTEGER DEFAULT (strftime('%s', 'now')))");

            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_DECK_CARDS + " (" +
                    COLUMN_DECK_CARDS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DECK_REF_ID + " INTEGER, " +
                    COLUMN_CARD_REF_ID + " INTEGER, " +
                    COLUMN_ADDED_AT + " INTEGER DEFAULT (strftime('%s', 'now')), " +
                    "FOREIGN KEY(" + COLUMN_DECK_REF_ID + ") REFERENCES " + TABLE_DECKS + "(" + COLUMN_DECK_ID + ") ON DELETE CASCADE, " +
                    "FOREIGN KEY(" + COLUMN_CARD_REF_ID + ") REFERENCES " + TABLE_CARDS + "(" + COLUMN_CARD_ID + ") ON DELETE CASCADE, " +
                    "UNIQUE(" + COLUMN_DECK_REF_ID + ", " + COLUMN_CARD_REF_ID + "))");
        }
    }

    // ==================== МЕТОДЫ ДЛЯ КОЛОД ====================

    public List<Deck> getAllDecks() {
        List<Deck> decks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_DECKS + " ORDER BY " + COLUMN_DECK_UPDATED_AT + " DESC", null);
            while (cursor.moveToNext()) {
                Deck deck = new Deck();
                deck.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DECK_ID));
                deck.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DECK_TITLE));
                deck.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DECK_CREATED_AT));
                deck.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DECK_UPDATED_AT));
                decks.add(deck);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return decks;
    }

    public List<Deck> searchDecks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllDecks();
        }

        List<Deck> decks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String searchPattern = "%" + query.trim() + "%";
            String sql = "SELECT * FROM " + TABLE_DECKS +
                    " WHERE " + COLUMN_DECK_TITLE + " LIKE ?" +
                    " ORDER BY " + COLUMN_DECK_UPDATED_AT + " DESC";
            cursor = db.rawQuery(sql, new String[]{searchPattern});
            while (cursor.moveToNext()) {
                Deck deck = new Deck();
                deck.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DECK_ID));
                deck.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DECK_TITLE));
                deck.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DECK_CREATED_AT));
                deck.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DECK_UPDATED_AT));
                decks.add(deck);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return decks;
    }

    public long saveDeck(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_DECK_TITLE, title);
        cv.put(COLUMN_DECK_UPDATED_AT, System.currentTimeMillis() / 1000);
        return db.insert(TABLE_DECKS, null, cv);
    }

    public void deleteDeck(long deckId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DECKS, COLUMN_DECK_ID + " = ?", new String[]{String.valueOf(deckId)});
    }

    // ==================== МЕТОДЫ ДЛЯ КАРТОЧЕК ====================

    public List<Card> getAllCards() {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.rawQuery("SELECT * FROM " + TABLE_CARDS + " ORDER BY " + COLUMN_CARD_UPDATED_AT + " DESC", null);
            while (cursor.moveToNext()) {
                Card card = new Card();
                card.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_ID));
                card.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_TITLE));
                card.question = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_QUESTION));
                card.answer = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_ANSWER));
                card.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_CREATED_AT));
                card.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_UPDATED_AT));
                cards.add(card);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return cards;
    }

    public List<Card> getAvailableCardsForDeck(long deckId, String query) {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String searchCondition = "";
            String[] args;

            if (query != null && !query.trim().isEmpty()) {
                searchCondition = " AND c." + COLUMN_CARD_TITLE + " LIKE ?";
                args = new String[]{String.valueOf(deckId), "%" + query.trim() + "%"};
            } else {
                args = new String[]{String.valueOf(deckId)};
            }

            String sql = "SELECT c.* FROM " + TABLE_CARDS + " c" +
                    " WHERE c." + COLUMN_CARD_ID + " NOT IN (" +
                    " SELECT " + COLUMN_CARD_REF_ID + " FROM " + TABLE_DECK_CARDS +
                    " WHERE " + COLUMN_DECK_REF_ID + " = ?)" +
                    searchCondition +
                    " ORDER BY c." + COLUMN_CARD_UPDATED_AT + " DESC";

            cursor = db.rawQuery(sql, args);

            while (cursor.moveToNext()) {
                Card card = new Card();
                card.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_ID));
                card.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_TITLE));
                card.question = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_QUESTION));
                card.answer = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_ANSWER));
                card.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_CREATED_AT));
                card.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_UPDATED_AT));
                cards.add(card);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return cards;
    }

    public List<Card> getCardsByDeckId(long deckId) {
        List<Card> cards = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String sql = "SELECT c.* FROM " + TABLE_CARDS + " c" +
                    " INNER JOIN " + TABLE_DECK_CARDS + " dc ON c." + COLUMN_CARD_ID + " = dc." + COLUMN_CARD_REF_ID +
                    " WHERE dc." + COLUMN_DECK_REF_ID + " = ?" +
                    " ORDER BY dc." + COLUMN_ADDED_AT + " DESC";

            cursor = db.rawQuery(sql, new String[]{String.valueOf(deckId)});

            while (cursor.moveToNext()) {
                Card card = new Card();
                card.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_ID));
                card.title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_TITLE));
                card.question = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_QUESTION));
                card.answer = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CARD_ANSWER));
                card.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_CREATED_AT));
                card.updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CARD_UPDATED_AT));
                cards.add(card);
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return cards;
    }

    public boolean addCardToDeck(long deckId, long cardId) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_DECK_REF_ID, deckId);
            cv.put(COLUMN_CARD_REF_ID, cardId);
            cv.put(COLUMN_ADDED_AT, System.currentTimeMillis() / 1000);
            long result = db.insertOrThrow(TABLE_DECK_CARDS, null, cv);
            return result != -1;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Card already in deck or error: " + e.getMessage());
            return false;
        }
    }

    public boolean removeCardFromDeck(long deckId, long cardId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deleted = db.delete(TABLE_DECK_CARDS,
                COLUMN_DECK_REF_ID + " = ? AND " + COLUMN_CARD_REF_ID + " = ?",
                new String[]{String.valueOf(deckId), String.valueOf(cardId)});
        return deleted > 0;
    }

    public long saveCard(String title, String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CARD_TITLE, title);
        cv.put(COLUMN_CARD_QUESTION, question != null ? question : "");
        cv.put(COLUMN_CARD_ANSWER, answer != null ? answer : "");
        cv.put(COLUMN_CARD_UPDATED_AT, System.currentTimeMillis() / 1000);
        return db.insert(TABLE_CARDS, null, cv);
    }

    public void updateCard(long cardId, String title, String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CARD_TITLE, title);
        cv.put(COLUMN_CARD_QUESTION, question != null ? question : "");
        cv.put(COLUMN_CARD_ANSWER, answer != null ? answer : "");
        cv.put(COLUMN_CARD_UPDATED_AT, System.currentTimeMillis() / 1000);
        db.update(TABLE_CARDS, cv, COLUMN_CARD_ID + " = ?", new String[]{String.valueOf(cardId)});
    }

    public void deleteCardPermanently(long cardId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CARDS, COLUMN_CARD_ID + " = ?", new String[]{String.valueOf(cardId)});
    }

    public boolean isCardInDeck(long deckId, long cardId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT 1 FROM " + TABLE_DECK_CARDS +
                        " WHERE " + COLUMN_DECK_REF_ID + " = ? AND " + COLUMN_CARD_REF_ID + " = ?",
                new String[]{String.valueOf(deckId), String.valueOf(cardId)});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }
}