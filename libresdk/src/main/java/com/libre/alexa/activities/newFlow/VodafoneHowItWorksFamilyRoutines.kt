package com.libre.alexa.activities.newFlow

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.libre.alexa.R
import com.libre.alexa.luci.LSSDPNodes
import kotlinx.android.synthetic.main.toolbar_white_bg_with_settings.*
import kotlinx.android.synthetic.main.vodafone_how_it_woks_family_routine.*

class VodafoneHowItWorksFamilyRoutines : AppCompatActivity() {

    var bundle = Bundle()

    var node: LSSDPNodes? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vodafone_how_it_woks_family_routine)


        bundle = intent.extras!!
        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }


        llBack.setOnClickListener {
            onBackPressed()
        }


        btnNext.setOnClickListener {
            if (VodafoneHelloFamilyRoutinesActivity.vodafoneHelloFamilyRoutinesActivity != null) {
                VodafoneHelloFamilyRoutinesActivity.vodafoneHelloFamilyRoutinesActivity?.finish()
            }
            val intent = Intent(this@VodafoneHowItWorksFamilyRoutines, VodafoneAlexaDinnerTimeDeviceListActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("deviceDetails", node)
            intent.putExtras(bundle)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.left_to_right_anim_tranistion,
                    R.anim.right_to_left_anim_transition);
        }
    }
}