package com.example.android.notepad;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;

public class NoteEditor extends Activity {
    // For logging and debugging purposes
    private static final String TAG = "NoteEditor";
    String[] PROJECTION =
            new String[] {
                    NotePad.Notes._ID,
                    NotePad.Notes.COLUMN_NAME_TITLE,
                    NotePad.Notes.COLUMN_NAME_NOTE,
                    NotePad.Notes.COLUMN_NAME_BACKGROUND_COLOR // 新增的背景颜色列
            };

    // A label for the saved state of the activity
    private static final String ORIGINAL_CONTENT = "origContent";

    // This Activity can be started by more than one action. Each action is represented
    // as a "state" constant
    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    // Global mutable variables
    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private String mOriginalContent;

    /**
     * Defines a custom EditText View that draws lines between each line of text that is displayed.
     */
    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        // This constructor is used by LayoutInflater
        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);

            // Creates a Rect and a Paint object, and sets the style and color of the Paint object.
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int count = getLineCount();
            Rect r = mRect;
            Paint paint = mPaint;
            for (int i = 0; i < count; i++) {
                int baseline = getLineBounds(i, r);
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        // For an edit action:
        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();

            // For an insert or paste action:
        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                finish();
                return;
            }
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        } else {
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        mCursor = managedQuery(
                mUri, PROJECTION, null, null, null);

        if (Intent.ACTION_PASTE.equals(action)) {
            performPaste();
            mState = STATE_EDIT;
        }

        setContentView(R.layout.note_editor);

        mText = (EditText) findViewById(R.id.note);

        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCursor != null) {
            mCursor.requery();
            mCursor.moveToFirst();

            if (mState == STATE_EDIT) {
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            // Get the background color from the database
            int colBgColorIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_BACKGROUND_COLOR);
            String bgColor = mCursor.getString(colBgColorIndex);
            if (bgColor != null) {
                updateBackgroundColor(bgColor);
            }

            if (mOriginalContent == null) {
                mOriginalContent = note;
            }
        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null) {
            String text = mText.getText().toString();
            int length = text.length();

            if (isFinishing() && (length == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();
            } else if (mState == STATE_EDIT) {
                updateNote(text, null);
            } else if (mState == STATE_INSERT) {
                updateNote(text, text);
                mState = STATE_EDIT;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        if (mState == STATE_EDIT) {
            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        String savedNote = mCursor.getString(colNoteIndex);
        String currentNote = mText.getText().toString();
        if (savedNote.equals(currentNote)) {
            menu.findItem(R.id.menu_revert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_revert).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_save:
                String text = mText.getText().toString();
                updateNote(text, null);
                finish();
                break;
            case R.id.menu_delete:
                deleteNote();
                finish();
                break;
            case R.id.menu_revert:
                cancelNote();
                break;
            case R.id.menu_change_bg_color:
                // Trigger a color picker or show a dialog to pick the background color
                showColorPickerDialog();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private final void performPaste() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ContentResolver cr = getContentResolver();
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {
            String text = null;
            String title = null;
            ClipData.Item item = clip.getItemAt(0);
            Uri uri = item.getUri();

            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
                Cursor orig = cr.query(uri, PROJECTION, null, null, null);
                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }
                    orig.close();
                }
            }
            if (text == null) {
                text = item.coerceToText(this).toString();
            }
            updateNote(text, title);
        }
    }

    private final void updateNote(String text, String title) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());

        if (mState == STATE_INSERT) {
            if (title == null) {
                int length = text.length();
                title = text.substring(0, Math.min(30, length));
                if (length > 30) {
                    int lastSpace = title.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        title = title.substring(0, lastSpace);
                    }
                }
            }
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
            values.put(NotePad.Notes.COLUMN_NAME_BACKGROUND_COLOR, "#FFFFFF"); // 默认白色背景

            mUri = getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);
        } else {
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);
            getContentResolver().update(mUri, values, null, null);
        }
    }

    private final void cancelNote() {
        mText.setText(mOriginalContent);
    }

    private final void deleteNote() {
        if (mUri != null) {
            getContentResolver().delete(mUri, null, null);
        }
    }

    private void updateBackgroundColor(String color) {
        findViewById(R.id.noteEditorLayout).setBackgroundColor(Color.parseColor(color));
    }
    private void showColorPickerDialog() {
        // 定义颜色名称数组和对应的 Hex 值
        final String[] colorNames = {
                "Red", "Green", "Blue", "Yellow", "Purple", "Orange",
                "Pink", "Cyan", "Magenta", "Brown", "Grey", "Teal"
        };

        final String[] colorHexValues = {
                "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#800080", "#FFA500", // 红、绿、蓝、黄、紫、橙
                "#FFC0CB", "#00FFFF", "#FF00FF", "#A52A2A", "#808080", "#008080"  // 粉红、青、品红、棕、灰、青绿色
        };

        // 创建一个GridView的适配器，显示每个颜色块
        GridView gridView = new GridView(this);
        gridView.setNumColumns(2); // 设置为2列显示
        gridView.setVerticalSpacing(10);
        gridView.setHorizontalSpacing(10);
        gridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);

        // 使用颜色 Hex 值来创建一个自定义的颜色选择适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, colorNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // 使用自定义的布局来显示颜色块
                if (convertView == null) {
                    convertView = new View(getContext());
                }

                // 设置颜色块的背景为对应的 Hex 值
                convertView.setBackgroundColor(Color.parseColor(colorHexValues[position]));
                convertView.setLayoutParams(new GridView.LayoutParams(150, 150)); // 设置颜色块的大小
                return convertView;
            }
        };

        gridView.setAdapter(adapter);

        // 创建AlertDialog，显示GridView
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择背景颜色");
        builder.setView(gridView);

        // 设置GridView的点击事件监听器
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 获取用户选择的颜色的 Hex 值
                String newColorHex = colorHexValues[position];

                // 更新背景颜色
                updateBackgroundColor(newColorHex);

                // 将背景颜色更新到数据库
                ContentValues values = new ContentValues();
                values.put(NotePad.Notes.COLUMN_NAME_BACKGROUND_COLOR, newColorHex);
                getContentResolver().update(mUri, values, null, null);
            }
        });

        builder.show();
    }



}
