package com.keyboardr.bluejay.bus.event;

import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.keyboardr.bluejay.PlaybackActivity;
import com.keyboardr.bluejay.R;

import org.greenrobot.eventbus.EventBus;

import java.util.EnumSet;

/**
 * Contains various errors that the playlist may have encountered that need to be displayed to
 * the user
 */
public class PlaylistErrorEvent {

  @IntDef({ErrorLevel.NONE, ErrorLevel.WARN, ErrorLevel.ERROR})
  public @interface ErrorLevel {
    int NONE = 0;
    int WARN = 1;
    int ERROR = 2;
  }

  public enum ErrorCode {
    NO_USB_OUTPUT(R.string.error_no_usb_audio, ErrorLevel.ERROR, 0),
    SOUND_CHECK(R.string.sound_check, ErrorLevel.NONE, R.string.end_sound_check) {
      @Override
      public void performRecoveryAction(PlaybackActivity activity) {
        //noinspection ConstantConditions
        activity.getSetlistFragment().endSet();
      }
    };

    @StringRes public final int message;
    @ErrorLevel public final int errorLevel;
    @StringRes public final int recoveryLabel;

    ErrorCode(@StringRes int message, @ErrorLevel int errorLevel, @StringRes int recoveryLabel) {
      this.message = message;
      this.errorLevel = errorLevel;
      this.recoveryLabel = recoveryLabel;
    }

    public void performRecoveryAction(PlaybackActivity activity) {
    }
  }

  public static final PlaylistErrorEvent EMPTY = new PlaylistErrorEvent(EnumSet.noneOf(ErrorCode
      .class));

  @NonNull
  public static PlaylistErrorEvent getCurrent(EventBus bus) {
    PlaylistErrorEvent existing = bus.getStickyEvent(PlaylistErrorEvent.class);
    if (existing == null) {
      existing = EMPTY;
    }
    return existing;
  }

  public static void setErrorEnabled(EventBus bus, ErrorCode errorCode, boolean enabled) {
    if (enabled) {
      addError(bus, errorCode);
    } else {
      removeError(bus, errorCode);
    }
  }

  public static void addError(EventBus bus, ErrorCode errorCode) {
    PlaylistErrorEvent current = getCurrent(bus);
    if (!current.errorCodes.contains(errorCode)) {
      bus.postSticky(current.addError(errorCode));
    }
  }

  public static void removeError(EventBus bus, ErrorCode errorCode) {
    PlaylistErrorEvent current = getCurrent(bus);
    if (current.errorCodes.contains(errorCode)) {
      bus.postSticky(current.removeError(errorCode));
    }
  }

  private final EnumSet<ErrorCode> errorCodes;

  private PlaylistErrorEvent(@NonNull EnumSet<ErrorCode> errorCodes) {
    this.errorCodes = errorCodes;
  }

  @CheckResult
  public PlaylistErrorEvent addError(ErrorCode newError) {
    EnumSet<ErrorCode> errorCodes = EnumSet.copyOf(this.errorCodes);
    errorCodes.add(newError);
    return new PlaylistErrorEvent(errorCodes);
  }

  @CheckResult
  public PlaylistErrorEvent removeError(ErrorCode error) {
    if (getErrorCount() == 1 && getTopError() == error) {
      return EMPTY;
    }
    EnumSet<ErrorCode> errorCodes = EnumSet.copyOf(this.errorCodes);
    errorCodes.remove(error);
    return new PlaylistErrorEvent(errorCodes);
  }

  @Nullable
  public ErrorCode getTopError() {
    for (ErrorCode errorCode : ErrorCode.values()) {
      if (errorCodes.contains(errorCode)) {
        return errorCode;
      }
    }
    return null;
  }

  public int getErrorCount() {
    return errorCodes.size();
  }

}
