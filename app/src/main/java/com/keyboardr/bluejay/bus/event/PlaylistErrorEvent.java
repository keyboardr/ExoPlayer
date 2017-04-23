package com.keyboardr.bluejay.bus.event;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.support.annotation.CheckResult;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

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
    NO_USB_OUTPUT(R.string.error_no_usb_audio, ErrorLevel.ERROR, null);

    @StringRes public final int message;
    @ErrorLevel public final int errorLevel;
    @Nullable private final Class<? extends BroadcastReceiver> recoveryReceiver;

    ErrorCode(@StringRes int message, @ErrorLevel int errorLevel, @Nullable Class<? extends
        BroadcastReceiver> recoveryReceiver) {
      this.message = message;
      this.errorLevel = errorLevel;
      this.recoveryReceiver = recoveryReceiver;
    }

    public void performRecoveryAction(@NonNull Context context) {
      context.sendBroadcast(new Intent(context, recoveryReceiver));
    }

    public int getRecoveryActionLabel(@NonNull Context context) {
      if (recoveryReceiver != null) {
        try {
          ActivityInfo receiverInfo = context.getPackageManager().getReceiverInfo(
              new ComponentName(context, recoveryReceiver),
              PackageManager.GET_META_DATA);
          return receiverInfo.labelRes;
        } catch (PackageManager.NameNotFoundException e) {
          e.printStackTrace();
        }
      }
      return 0;
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
