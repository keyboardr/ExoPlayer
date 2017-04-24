package com.keyboardr.bluejay.ui.playlist;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.keyboardr.bluejay.PlaybackActivity;
import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.bus.event.PlaylistErrorEvent;
import com.keyboardr.bluejay.util.FragmentUtils;

/**
 * Displays {@link com.keyboardr.bluejay.bus.event.PlaylistErrorEvent.ErrorCode ErrorCodes} and
 * provides method to fix if possible
 */

public class ErrorFragment extends Fragment {

  public static ErrorFragment newInstance(@NonNull PlaylistErrorEvent.ErrorCode errorCode) {
    ErrorFragment errorFragment = new ErrorFragment();
    Bundle args = new Bundle();
    args.putSerializable(ARG_ERROR_CODE, errorCode);
    errorFragment.setArguments(args);
    return errorFragment;
  }

  private static final String ARG_ERROR_CODE = "errorCode";

  private PlaylistErrorEvent.ErrorCode errorCode;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, PlaybackActivity.class);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    errorCode = (PlaylistErrorEvent.ErrorCode) getArguments().getSerializable(ARG_ERROR_CODE);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_error, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    view.getBackground().setLevel(errorCode.errorLevel);
    ((TextView) view.findViewById(R.id.error_text)).setText(errorCode.message);
    if (errorCode.recoveryLabel > 0) {
      Button recoveryButton = (Button) view.findViewById(R.id.error_button);
      recoveryButton.setText(errorCode.recoveryLabel);
      recoveryButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          errorCode.performRecoveryAction((PlaybackActivity) getActivity());
        }
      });
      recoveryButton.setVisibility(View.VISIBLE);
    }
  }

  public PlaylistErrorEvent.ErrorCode getErrorCode() {
    return errorCode;
  }
}
