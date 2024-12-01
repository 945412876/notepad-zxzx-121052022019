/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    // The columns needed by the cursor adapter
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_NOTE, // 2
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 3

    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_CONTENT = 2;
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 3;


    private SimpleCursorAdapter adapter;
    private Cursor currentCursor;
    private boolean sortByTitle = true;  // 初始排序方式：按标题排序

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        // Initialize the cursor adapter
        setupAdapter();
    }

    /**
     * Method to set up the adapter
     */
    private void setupAdapter() {
        // 设置排序条件
        String orderBy = sortByTitle ? NotePad.Notes.COLUMN_NAME_TITLE + " ASC" : NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";

        currentCursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                orderBy
        );

        // The names of the cursor columns to display in the view
        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
        int[] viewIDs = { android.R.id.text1, R.id.timestamp_text };

        adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                currentCursor,
                dataColumns,
                viewIDs
        ) {
            @Override
            public void bindView(View view, Context context, Cursor cursor) {
                super.bindView(view, context, cursor);

                // 获取时间戳并格式化
                long modificationTimestamp = cursor.getLong(COLUMN_INDEX_MODIFICATION_DATE);
                String formattedTimestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", modificationTimestamp).toString();

                // 获取时间戳TextView并设置格式化后的时间戳
                TextView timestampTextView = (TextView) view.findViewById(R.id.timestamp_text);
                timestampTextView.setText(formattedTimestamp);
            }
        };

        setListAdapter(adapter);
    }

    /**
     * Method to filter the notes based on the query
     */
    private void filterNotes(String query) {
        String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " + NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
        String[] selectionArgs = { "%" + query + "%", "%" + query + "%" };

        // 设置排序条件
        String orderBy = sortByTitle ? NotePad.Notes.COLUMN_NAME_TITLE + " ASC" : NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";

        currentCursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                orderBy
        );

        adapter.changeCursor(currentCursor);
    }

    /**
     * Handle search query change
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            mPasteItem.setEnabled(false);
        }

        return true;
    }

    /**
     * Prepare options menu (for clipboard handling)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // Set up the search view
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // When query is submitted, filter notes
                filterNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // As the user types, filter notes
                filterNotes(newText);
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                // Launches a new Activity using an Intent to add a new note
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;
            case R.id.menu_paste:
                // Launches a new Activity using an Intent to paste clipboard data
                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
                return true;
            case R.id.menu_sort_by_title:
                // Sort by title
                if (!sortByTitle) {
                    sortByTitle = true;
                    setupAdapter(); // Reload notes with the new sort order
                }
                return true;
            case R.id.menu_sort_by_date:
                // Sort by modification date
                if (sortByTitle) {
                    sortByTitle = false;
                    setupAdapter(); // Reload notes with the new sort order
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));


        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                                        Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        switch (item.getItemId()) {
        case R.id.context_open:
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
//BEGIN_INCLUDE(copy)
        case R.id.context_copy:
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // Copies the notes URI to the clipboard. In effect, this copies the note itself
            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                             // label for the clip
                    noteUri)                            // the URI
            );

            // Returns to the caller and skips further processing.
            return true;
//END_INCLUDE(copy)
        case R.id.context_delete:

            // Deletes the note from the provider by passing in a URI in note ID format.
            // Please see the introductory note about performing provider operations on the
            // UI thread.
            getContentResolver().delete(
                noteUri,  // The URI of the provider
                null,     // No where clause is needed, since only a single note ID is being
                          // passed in.
                null      // No where clause is used, so no where arguments are needed.
            );

            // Returns to the caller and skips further processing.
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
