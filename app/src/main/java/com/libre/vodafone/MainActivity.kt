package com.libre.vodafone

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import com.libre.alexa.activities.newFlow.VodafoneLoginRegisterWebViewActivity


class MainActivity : AppCompatActivity() {

    private var customTwoAlertDialog: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent =
            Intent(this@MainActivity, VodafoneLoginRegisterWebViewActivity::class.java)
        startActivity(intent)
        finish()
    }


}