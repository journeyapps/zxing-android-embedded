/*
 * Copyright (C) 2008 ZXing authors
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

package com.google.zxing.client.android;

/**
 * The main settings activity.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class PreferencesActivity {
  public static final String KEY_PLAY_BEEP = "zxing_preferences_play_beep";
  public static final String KEY_VIBRATE = "zxing_preferences_vibrate";
  public static final String KEY_FRONT_LIGHT_MODE = "zxing_preferences_front_light_mode";
  public static final String KEY_AUTO_FOCUS = "zxing_preferences_auto_focus";
  public static final String KEY_INVERT_SCAN = "zxing_preferences_invert_scan";

  public static final String KEY_DISABLE_CONTINUOUS_FOCUS = "zxing_preferences_disable_continuous_focus";
  public static final String KEY_DISABLE_EXPOSURE = "zxing_preferences_disable_exposure";
  public static final String KEY_DISABLE_METERING = "zxing_preferences_disable_metering";
  public static final String KEY_DISABLE_BARCODE_SCENE_MODE = "zxing_preferences_disable_barcode_scene_mode";
}
