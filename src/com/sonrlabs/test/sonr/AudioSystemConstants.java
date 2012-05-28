/***************************************************************************
 * Copyright 2012 by SONR
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

/**
 *  TODO: replace this junk with javadoc for this class.
 */
public class AudioSystemConstants {
   //@formatter:off
   /*
   public static native int setDeviceConnectionState(int device, int state, String device_address);
   public static native int getDeviceConnectionState(int device, String device_address);
   public static native int setPhoneState(int state);
   public static native int setRingerMode(int mode, int mask);
   public static native int setForceUse(int usage, int config);
   public static native int getForceUse(int usage);
   public static native int initStreamVolume(int stream, int indexMin, int indexMax);
   public static native int setStreamVolumeIndex(int stream, int index);
   public static native int getStreamVolumeIndex(int stream);
   public static native int getDevicesForStream(int stream);
    */ 
   //@formatter:on
   // output devices, be sure to update AudioManager.java also
   public static final int DEVICE_OUT_EARPIECE = 0x1;
   public static final int DEVICE_OUT_SPEAKER = 0x2;
   public static final int DEVICE_OUT_WIRED_HEADSET = 0x4;
   public static final int DEVICE_OUT_WIRED_HEADPHONE = 0x8;
   public static final int DEVICE_OUT_BLUETOOTH_SCO = 0x10;
   public static final int DEVICE_OUT_BLUETOOTH_SCO_HEADSET = 0x20;
   public static final int DEVICE_OUT_BLUETOOTH_SCO_CARKIT = 0x40;
   public static final int DEVICE_OUT_BLUETOOTH_A2DP = 0x80;
   public static final int DEVICE_OUT_BLUETOOTH_A2DP_HEADPHONES = 0x100;
   public static final int DEVICE_OUT_BLUETOOTH_A2DP_SPEAKER = 0x200;
   public static final int DEVICE_OUT_AUX_DIGITAL = 0x400;
   public static final int DEVICE_OUT_ANLG_DOCK_HEADSET = 0x800;
   public static final int DEVICE_OUT_DGTL_DOCK_HEADSET = 0x1000;
   public static final int DEVICE_OUT_DEFAULT = 0x8000;
   // input devices
   public static final int DEVICE_IN_COMMUNICATION = 0x10000;
   public static final int DEVICE_IN_AMBIENT = 0x20000;
   public static final int DEVICE_IN_BUILTIN_MIC1 = 0x40000;
   public static final int DEVICE_IN_BUILTIN_MIC2 = 0x80000;
   public static final int DEVICE_IN_MIC_ARRAY = 0x100000;
   public static final int DEVICE_IN_BLUETOOTH_SCO_HEADSET = 0x200000;
   public static final int DEVICE_IN_WIRED_HEADSET = 0x400000;
   public static final int DEVICE_IN_AUX_DIGITAL = 0x800000;
   public static final int DEVICE_IN_DEFAULT = 0x80000000;

   // device states, must match AudioSystem::device_connection_state
   public static final int DEVICE_STATE_UNAVAILABLE = 0;
   public static final int DEVICE_STATE_AVAILABLE = 1;
   private static final int NUM_DEVICE_STATES = 1;

   // phone state, match audio_mode???
   public static final int PHONE_STATE_OFFCALL = 0;
   public static final int PHONE_STATE_RINGING = 1;
   public static final int PHONE_STATE_INCALL = 2;

   // device categories config for setForceUse, must match AudioSystem::forced_config
   public static final int FORCE_NONE = 0;
   public static final int FORCE_SPEAKER = 1;
   public static final int FORCE_HEADPHONES = 2;
   public static final int FORCE_BT_SCO = 3;
   public static final int FORCE_BT_A2DP = 4;
   public static final int FORCE_WIRED_ACCESSORY = 5;
   public static final int FORCE_BT_CAR_DOCK = 6;
   public static final int FORCE_BT_DESK_DOCK = 7;
   public static final int FORCE_ANALOG_DOCK = 8;
   public static final int FORCE_DIGITAL_DOCK = 9;
   private static final int NUM_FORCE_CONFIG = 10;
   public static final int FORCE_DEFAULT = FORCE_NONE;

   // usage for setForceUse, must match AudioSystem::force_use
   public static final int FOR_COMMUNICATION = 0;
   public static final int FOR_MEDIA = 1;
   public static final int FOR_RECORD = 2;
   public static final int FOR_DOCK = 3;
   private static final int NUM_FORCE_USE = 4;
   // --- AudioSystem constants from android.media.AudioSystem
}
