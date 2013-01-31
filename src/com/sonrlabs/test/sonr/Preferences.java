/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


/**
 *  Utility methods for getting and setting preferences.
 */
class Preferences {
   private static final String SHARED_PREF_NAME = "SONR";
   
   static final String N_A = "N_A";

   static String getPreference(Context context, String key, String defaultValue) {
      SharedPreferences settings = getPreferences(context);
      return settings.getString(key, defaultValue);
   }

   static boolean getPreference(Context context, String key, boolean defaultValue) {
      SharedPreferences settings = getPreferences(context);
      return settings.getBoolean(key, defaultValue);
   }
   
   static int getPreference(Context context, String key, int defaultValue) {
      SharedPreferences settings = getPreferences(context);
      return settings.getInt(key, defaultValue);
   }

   static void savePreference(Context context, String key, String value) {
      SharedPreferences.Editor editor = getEditor(context);
      editor.putString(key, value);
      editor.commit();
   }

   static void savePreference(Context context, String key, boolean value) {
      SharedPreferences.Editor editor = getEditor(context);
      editor.putBoolean(key, value);
      editor.commit();
   }
   
   static void savePreference(Context context, String key, int value) {
      SharedPreferences.Editor editor = getEditor(context);
      editor.putInt(key, value);
      editor.commit();
   }
   
   
   /* Three unused methods, examples of working with preference vectors. */
   
   /**
    * Retrieves a boolean array from SharedPreferences.
    * If a value was not in the array, false will be in its place.
    *
    * Returns null if not even one boolean is found to be true.
    */
   static boolean[] getFlags(Context context, int key) {
      boolean atLeastOneTrue = false;
      String[] optionStrings = context.getResources().getStringArray(key);
      boolean[] response = new boolean[optionStrings.length];
      SharedPreferences settings = getPreferences(context);
      for (int i = 0; i < optionStrings.length; i++) {
         boolean storedValue = settings.getBoolean(String.format("%s.%s", key, i), false);
         if (storedValue) {
            atLeastOneTrue = true;
         }
         response[i] = storedValue;
      }
      return atLeastOneTrue ? response : null;
   }

   void saveFlags(Context context, int key, boolean[] vals) {
      SharedPreferences.Editor editor = getEditor(context);
      for (int i = 0; i < vals.length; i++) {
         editor.putBoolean(String.format("%s.%s", key, i), vals[i]);
      }
      editor.commit();
   }
   
   static void removeFlag(Context context, int key, int index) {
      SharedPreferences.Editor editor = getEditor(context);
      editor.remove(String.format("%s.%s", key, index));
      editor.clear();
   }

   
   private static Editor getEditor(Context context) {
      return getPreferences(context).edit();
   }

   private static SharedPreferences getPreferences(Context context) {
      return context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
   }
   
}
