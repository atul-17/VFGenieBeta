package com.libre.alexa.utils;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.libre.alexa.utils.interfaces.OnNextButtonClickFromAlertDialogInterface;


public class DynamicAlertDialogBox {

    private AppCompatActivity appCompatActivity;

    public void showAlertDialog(Context context, String msg, String title, String buttonName,
                                final OnNextButtonClickFromAlertDialogInterface
                                        nextButtonClickFromAlertDialogInterface) {
        appCompatActivity = (AppCompatActivity) context;
        if (!appCompatActivity.isFinishing()) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton(buttonName, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            nextButtonClickFromAlertDialogInterface.onButtonClickFromAlertDialog();
                        }
                    }).create().show();
        }
    }
}
