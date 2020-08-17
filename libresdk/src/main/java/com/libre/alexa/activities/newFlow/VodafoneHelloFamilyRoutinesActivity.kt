package com.libre.alexa.activities.newFlow

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.libre.alexa.R
import com.libre.alexa.luci.LSSDPNodes
import kotlinx.android.synthetic.main.activity_vodafone_hello_family_routines.*

class VodafoneHelloFamilyRoutinesActivity : AppCompatActivity() {

    lateinit var toolbar: View

    lateinit var llBack: LinearLayout

    lateinit var ivAppSettings: AppCompatImageView

    var bundle = Bundle()

    var node: LSSDPNodes? = null

    companion object {
        var vodafoneHelloFamilyRoutinesActivity: VodafoneHelloFamilyRoutinesActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vodafoneHelloFamilyRoutinesActivity = this


        setContentView(R.layout.activity_vodafone_hello_family_routines)

        bundle = intent.extras!!
        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }
        toolbar = findViewById(R.id.toolbar)

        llBack = toolbar.findViewById(R.id.llBack)


        llBack.setOnClickListener {
            onBackPressed()
        }



        btnNext.setOnClickListener {
            val intent = Intent(this@VodafoneHelloFamilyRoutinesActivity, VodafoneHowItWorksFamilyRoutines::class.java)
            val bundle = Bundle()
            bundle.putSerializable("deviceDetails", node)
            intent.putExtras(bundle)
            startActivity(intent)
            overridePendingTransition(R.anim.left_to_right_anim_tranistion,
                    R.anim.right_to_left_anim_transition);
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        vodafoneHelloFamilyRoutinesActivity = null
    }

}