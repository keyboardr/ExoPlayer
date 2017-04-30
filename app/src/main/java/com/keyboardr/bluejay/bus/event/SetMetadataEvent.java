package com.keyboardr.bluejay.bus.event;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.keyboardr.bluejay.model.SetMetadata;

import org.greenrobot.eventbus.EventBus;

/**
 * Holder of {@link com.keyboardr.bluejay.model.SetMetadata}
 */

public class SetMetadataEvent {
  @Nullable
  public final SetMetadata setMetadata;

  @Nullable
  public static SetMetadata getSetMetadata(@NonNull EventBus bus) {
    SetMetadataEvent oldMetadataEvent = bus.getStickyEvent(SetMetadataEvent.class);
    return oldMetadataEvent != null ? oldMetadataEvent.setMetadata : null;
  }

  public SetMetadataEvent(@Nullable SetMetadata setMetadata) {
    this.setMetadata = setMetadata;
  }
}
