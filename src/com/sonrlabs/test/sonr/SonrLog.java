/***************************************************************************
 * Copyright (c) 2011, 2012 by Sonr Labs Inc (http://www.sonrlabs.com)
 *
 *You can redistribute this program and/or modify it under the terms of the GNU General Public License v. 2.0 as published by the Free Software Foundation
 *This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 **************************************************************************/

package com.sonrlabs.test.sonr;

import android.util.Log;

/**
 * This class exists to divert the log, filter TAGs, etc.
 *  TODO: add support for release, debug modes
 */
public class SonrLog {

   public static void d(String tag, String message) {
      if (verifyTag(tag)) {
         Log.d(tag, message);
      }
   }
   
   public static void e(String tag, String message) {
      if (verifyTag(tag)) {
         Log.e(tag, message);
      }
   }
   
   public static void i(String tag, String message) {
      if (verifyTag(tag)) {
         Log.i(tag, message);
      }
   }
   
   public static void v(String tag, String message) {
      if (verifyTag(tag)) {
         Log.v(tag, message);
      }
   }
 
   public static void w(String tag, String message) {
      if (verifyTag(tag)) {
         Log.w(tag, message);
      }
   }
 
   static boolean verifyTag(String tag) {
      return true; //(ToggleSONR.class.getSimpleName().equals(tag));
   }
   
}
