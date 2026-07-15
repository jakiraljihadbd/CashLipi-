package com.jrappspot.cashlipi.utils;

import android.content.Context;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import com.jrappspot.cashlipi.R;

/**
 * EmojiDrawable — Central helper for emoji PNG resources.
 *
 * সমস্ত ইমোজি PNG ফাইল রয়েছে:
 *   res/drawable/emojis/emoji_*.png
 *
 * ব্যবহার (Java):
 *   EmojiDrawable.set(imageView, EmojiDrawable.MONEY_BAG);
 *   int resId = EmojiDrawable.res(EmojiDrawable.CHECK_MARK_GREEN);
 *
 * ব্যবহার (XML):
 *   android:src="@drawable/emoji_money_bag"
 */
public class EmojiDrawable {

    // ── Emoji drawable resource IDs ──────────────────────────────────────────

    public static final int CHECK_MARK_GREEN     = R.drawable.emoji_check_mark_green;
    public static final int FOLDER               = R.drawable.emoji_folder;
    public static final int WARNING              = R.drawable.emoji_warning;
    public static final int ROCKET               = R.drawable.emoji_rocket;
    public static final int ART                  = R.drawable.emoji_art;
    public static final int TEST_TUBE            = R.drawable.emoji_test_tube;
    public static final int PACKAGE              = R.drawable.emoji_package;
    public static final int WRENCH               = R.drawable.emoji_wrench;
    public static final int HEART_RED            = R.drawable.emoji_heart_red;
    public static final int BANK                 = R.drawable.emoji_bank;
    public static final int CREDIT_CARD          = R.drawable.emoji_credit_card;
    public static final int DOLLAR_BILL          = R.drawable.emoji_dollar_bill;
    public static final int BLUE_DIAMOND         = R.drawable.emoji_blue_diamond;
    public static final int FLOPPY_DISK          = R.drawable.emoji_floppy_disk;
    public static final int AIRPLANE             = R.drawable.emoji_airplane;
    public static final int CLOUD                = R.drawable.emoji_cloud;
    public static final int GREEN_HEART          = R.drawable.emoji_green_heart;
    public static final int LEDGER               = R.drawable.emoji_ledger;
    public static final int PURPLE_HEART         = R.drawable.emoji_purple_heart;
    public static final int MEMO                 = R.drawable.emoji_memo;
    public static final int TRASH                = R.drawable.emoji_trash;
    public static final int BOOK_RED             = R.drawable.emoji_book_red;
    public static final int BOOK_GREEN           = R.drawable.emoji_book_green;
    public static final int CROSS_MARK           = R.drawable.emoji_cross_mark;
    public static final int ARROWS_CLOCKWISE     = R.drawable.emoji_arrows_clockwise;
    public static final int MONEY_BAG            = R.drawable.emoji_money_bag;
    public static final int CAMERA               = R.drawable.emoji_camera;
    public static final int NO_ENTRY             = R.drawable.emoji_no_entry;
    public static final int INBOX_TRAY           = R.drawable.emoji_inbox_tray;
    public static final int CRESCENT_MOON        = R.drawable.emoji_crescent_moon;
    public static final int SUN                  = R.drawable.emoji_sun;
    public static final int X_MARK              = R.drawable.emoji_x_mark;
    public static final int OUTBOX_TRAY          = R.drawable.emoji_outbox_tray;
    public static final int PENCIL               = R.drawable.emoji_pencil;
    public static final int TROPHY               = R.drawable.emoji_trophy;
    public static final int THUMBS_UP            = R.drawable.emoji_thumbs_up;
    public static final int CHART_UP             = R.drawable.emoji_chart_up;
    public static final int MONEY_WITH_WINGS     = R.drawable.emoji_money_with_wings;
    public static final int BAR_CHART            = R.drawable.emoji_bar_chart;
    public static final int BULB                 = R.drawable.emoji_bulb;
    public static final int RED_CIRCLE           = R.drawable.emoji_red_circle;
    public static final int DOOR                 = R.drawable.emoji_door;
    public static final int CHECK_MARK           = R.drawable.emoji_check_mark;
    public static final int X_MARK2             = R.drawable.emoji_x_mark2;
    public static final int CALENDAR             = R.drawable.emoji_calendar;
    public static final int UNLOCKED             = R.drawable.emoji_unlocked;
    public static final int LOCKED               = R.drawable.emoji_locked;
    public static final int PAUSE                = R.drawable.emoji_pause;
    public static final int PAGE                 = R.drawable.emoji_page;
    public static final int EMPTY_MAILBOX        = R.drawable.emoji_empty_mailbox;
    public static final int ROBOT                = R.drawable.emoji_robot;
    public static final int CLIPBOARD            = R.drawable.emoji_clipboard;
    public static final int HOURGLASS            = R.drawable.emoji_hourglass;
    public static final int SUN_SMALL_CLOUD      = R.drawable.emoji_sun_small_cloud;
    public static final int CITYSCAPE_DUSK       = R.drawable.emoji_cityscape_dusk;
    public static final int LABEL                = R.drawable.emoji_label;
    public static final int LIGHTNING            = R.drawable.emoji_lightning;
    public static final int ABACUS               = R.drawable.emoji_abacus;
    public static final int BOOKS                = R.drawable.emoji_books;
    public static final int PUSHPIN              = R.drawable.emoji_pushpin;
    public static final int BUST_IN_SILHOUETTE   = R.drawable.emoji_bust_in_silhouette;
    public static final int ALARM_CLOCK          = R.drawable.emoji_alarm_clock;
    public static final int MAGNIFIER            = R.drawable.emoji_magnifier;
    public static final int INPUT_NUMBERS        = R.drawable.emoji_input_numbers;
    public static final int GEAR                 = R.drawable.emoji_gear;
    public static final int TELEPHONE            = R.drawable.emoji_telephone;
    public static final int NO_MOBILE            = R.drawable.emoji_no_mobile;
    public static final int CLOCK1               = R.drawable.emoji_clock1;
    public static final int BLUE_CIRCLE          = R.drawable.emoji_blue_circle;
    public static final int OPEN_FOLDER          = R.drawable.emoji_open_folder;
    public static final int LINK                 = R.drawable.emoji_link;
    public static final int CALENDAR_SPIRAL      = R.drawable.emoji_calendar_spiral;
    public static final int SPIRAL_CALENDAR      = R.drawable.emoji_spiral_calendar;
    public static final int YELLOW_CIRCLE        = R.drawable.emoji_yellow_circle;
    public static final int GREEN_CIRCLE         = R.drawable.emoji_green_circle;
    public static final int PINK_HEART           = R.drawable.emoji_pink_heart;
    public static final int PURPLE_CIRCLE        = R.drawable.emoji_purple_circle;
    public static final int SHIELD               = R.drawable.emoji_shield;
    public static final int ARROW_RIGHT          = R.drawable.emoji_arrow_right;
    public static final int KEY                  = R.drawable.emoji_key;
    public static final int CARD_INDEX           = R.drawable.emoji_card_index;
    public static final int INDEX_POINTING_UP    = R.drawable.emoji_index_pointing_up;
    public static final int GLOBE                = R.drawable.emoji_globe;
    public static final int DOLLAR_SIGN          = R.drawable.emoji_dollar_sign;
    public static final int SPARKLING_HEART      = R.drawable.emoji_sparkling_heart;
    public static final int SPEECH_BUBBLE        = R.drawable.emoji_speech_bubble;
    public static final int BACKSPACE            = R.drawable.emoji_backspace;
    public static final int MOBILE_PHONE         = R.drawable.emoji_mobile_phone;
    public static final int MAN                  = R.drawable.emoji_man;
    public static final int LAPTOP               = R.drawable.emoji_laptop;

    // ── Utility methods ───────────────────────────────────────────────────────

    /**
     * Set emoji PNG onto an ImageView.
     * Example: EmojiDrawable.set(myImageView, EmojiDrawable.MONEY_BAG);
     */
    public static void set(ImageView view, @DrawableRes int emojiRes) {
        view.setImageResource(emojiRes);
    }

    /**
     * Return the drawable resource id (for use in setCompoundDrawablesWithIntrinsicBounds, etc.)
     */
    public static @DrawableRes int res(@DrawableRes int emojiRes) {
        return emojiRes;
    }
}
