package com.keyboardr.bluejay.ui.history;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.provider.SetlistContract;

/**
 * Dialog shown to rename a setlist in history
 */

public class SetlistRenameDialogFragment extends DialogFragment {
  private static final String ARG_NAME = "name";
  private static final String ARG_ID = "id";

  public static SetlistRenameDialogFragment newInstance(@NonNull String name, long id) {
    SetlistRenameDialogFragment fragment = new SetlistRenameDialogFragment();
    Bundle args = new Bundle();
    args.putString(ARG_NAME, name);
    args.putLong(ARG_ID, id);
    fragment.setArguments(args);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    @SuppressLint("InflateParams")
    View view = LayoutInflater.from(builder.getContext())
        .inflate(R.layout.fragment_rename, null);
    final EditText editText = (EditText) view.findViewById(R.id.rename_edit_text);
    if (savedInstanceState == null) {
      editText.setText(getName());
      editText.setSelection(editText.getText().length());
    }
    editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        doRename(textView);
        dismiss();
        return true;
      }
    });
    builder.setView(view);
    builder.setTitle(getString(R.string.rename_title, getName()));
    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        doRename(editText);
        dialogInterface.dismiss();
      }
    });
    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        dialogInterface.cancel();
      }
    });
    return builder.create();
  }

  @NonNull
  private String getName() {
    //noinspection ConstantConditions
    return getArguments().getString(ARG_NAME);
  }

  private void doRename(TextView text) {
    new RenameTask(getContext().getContentResolver(), getArguments().getLong(ARG_ID))
        .execute(text.getText().toString());
  }

  private static class RenameTask extends AsyncTask<String, Void, Void> {
    private final ContentResolver contentResolver;
    private final long id;

    public RenameTask(@NonNull ContentResolver contentResolver, long id) {
      this.contentResolver = contentResolver;
      this.id = id;
    }

    @Override
    protected Void doInBackground(String... params) {
      ContentValues values = new ContentValues();
      values.put(SetlistContract.NAME, params[0]);
      contentResolver.update(ContentUris.withAppendedId(SetlistContract.CONTENT_URI, id),
          values, null, null);
      return null;
    }
  }
}
