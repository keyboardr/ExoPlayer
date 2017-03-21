package com.keyboardr.bluejay.ui.shortlists;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
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
import com.keyboardr.bluejay.model.Shortlist;
import com.keyboardr.bluejay.provider.ShortlistManager;

/**
 * Dialog for renaming a shortlist
 */

public class ShortlistRenameDialogFragment extends DialogFragment {

  private static final String ARG_SHORTLIST = "shortlist";

  public static ShortlistRenameDialogFragment newInstance(@NonNull Shortlist shortlist) {
    ShortlistRenameDialogFragment fragment = new ShortlistRenameDialogFragment();
    Bundle args = new Bundle();
    args.putParcelable(ARG_SHORTLIST, shortlist);
    fragment.setArguments(args);
    return fragment;
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    @SuppressLint("InflateParams")
    View view = LayoutInflater.from(builder.getContext())
        .inflate(R.layout.fragment_shortlist_rename, null);
    final EditText editText = (EditText) view.findViewById(R.id.rename_edit_text);
    if (savedInstanceState == null) {
      editText.setText(getShortlist().getName());
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
    builder.setTitle(getString(R.string.edit_shortlist_title, getShortlist().getName()));
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

  private void doRename(TextView text) {
    ShortlistManager.getInstance(getContext()).renameShortlist(getShortlist(),
        text.getText().toString());
  }

  private Shortlist getShortlist() {
    //noinspection ConstantConditions
    return getArguments().getParcelable(ARG_SHORTLIST);
  }
}
