package com.libre.alexa.activities.newFlow

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.libre.alexa.R
import kotlinx.android.synthetic.main.activity_vdf_t_and_c_agreement_layout.*

class VdfTermsAndConditionsActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vdf_t_and_c_agreement_layout)

        btnIAgree.setOnClickListener {
            saveUserTAndCAgreement()
            //take the user to vdf webview activity
            val intent = Intent(this@VdfTermsAndConditionsActivity,VodafoneLoginRegisterWebViewActivity::class.java)
            startActivity(intent)
        }
        tvShowTAndC.setOnClickListener {

        }
    }

    fun saveUserTAndCAgreement(){
        val sharedPreferences = getSharedPreferences("Libre",Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isTAndCAgreed",true)
        editor.apply()
    }
}