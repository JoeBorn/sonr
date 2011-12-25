package com.sonrlabs.test.sonr.common;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogCommon {

   private static DialogInterface.OnClickListener doNothingListener = new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
         //do nothing
      }
   };

   public static void quickPopoutDialog(Context c, boolean cancellable, String msg, String buttonText) {
      AlertDialog.Builder builder = new AlertDialog.Builder(c);
      builder.setMessage(msg);
      builder.setCancelable(cancellable);
      builder.setNeutralButton(buttonText, doNothingListener);
      builder.create().show();
   }

}
