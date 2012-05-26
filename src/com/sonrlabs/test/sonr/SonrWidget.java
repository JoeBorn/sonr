/*
 * Copyright (C) 2009 Dan Walkes
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

package com.sonrlabs.test.sonr;

//import org.acra.ErrorReporter;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SonrWidget
      extends AppWidgetProvider {

   private static final String TAG = SonrWidget.class.getSimpleName();

   /**
    * Called when appwidget is loaded
    */
   @Override
   public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
      try {
         Log.d(TAG, "onUpdate");
         // do all updates within a service (we can keep alive to register
         // headset intents)
         context.startService(new Intent(context, SonrService.class));
      } catch (RuntimeException e) {
         e.printStackTrace();
         //ErrorReporter.getInstance().handleException(e);
      }
   }
}
