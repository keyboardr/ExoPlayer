package com.keyboardr.bluejay.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.ui.shortlists.ShortlistEditorFragment;

public class EditShortlistsActivity extends AppCompatActivity
    implements ShortlistEditorFragment.Holder {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_edit_shortlists);
  }

  @Override
  public void closeShortlistEditor() {
    finish();
  }
}
