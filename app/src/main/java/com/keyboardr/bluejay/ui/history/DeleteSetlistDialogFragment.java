package com.keyboardr.bluejay.ui.history;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.provider.SetlistContract;
import com.keyboardr.bluejay.provider.SetlistItemContract;

public class DeleteSetlistDialogFragment extends DialogFragment {
  private static final String ARG_NAME = "name";
  private static final String ARG_ID = "id";

  public static DeleteSetlistDialogFragment newInstance(@NonNull String name, long id) {
    DeleteSetlistDialogFragment fragment = new DeleteSetlistDialogFragment();
    Bundle args = new Bundle();
    args.putString(ARG_NAME, name);
    args.putLong(ARG_ID, id);
    fragment.setArguments(args);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    return new AlertDialog.Builder(getContext()).setTitle(R.string.delete_setlist)
        .setMessage(
            getString(R.string.confirm_delete_setlist, getArguments().getString(ARG_NAME)))
        .setPositiveButton(android.R.string.yes,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(
                    getContext().getContentResolver()) {
                };
                long id = getArguments().getLong(ARG_ID);
                asyncQueryHandler.startDelete(0, null, ContentUris.withAppendedId
                    (SetlistContract.CONTENT_URI, id), null, null);
                asyncQueryHandler.startDelete(0, null, SetlistItemContract.CONTENT_URI,
                    SetlistItemContract.SETLIST_ID + "=?", new String[]{Long.toString(id)});
                dialogInterface.dismiss();
              }
            }).setNegativeButton(android.R.string.cancel,
            new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
              }
            }).create();
  }
}
