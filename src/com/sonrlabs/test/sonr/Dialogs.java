package com.sonrlabs.test.sonr;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

class Dialogs {

   private static final DialogInterface.OnClickListener doNothingListener = new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialog, int id) {
      }
   };

   static void quickPopoutDialog(Context context, boolean cancellable, String msg, String buttonText) {
      AlertDialog.Builder builder = new AlertDialog.Builder(context);
      builder.setMessage(msg);
      builder.setCancelable(cancellable);
      builder.setNeutralButton(buttonText, doNothingListener);
      builder.create().show();
   }

}
