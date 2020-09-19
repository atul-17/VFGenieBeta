package com.libre.alexa.activities.newFlow

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.libre.alexa.R
import com.libre.alexa.luci.LSSDPNodes
import kotlinx.android.synthetic.main.activity_vdf_setting_up_family_routines_layout.*
import kotlinx.android.synthetic.main.toolbar_white_bg_with_settings.*

class VodafoneSettingUpFamilyRoutinesActivity : AppCompatActivity() {

    var bundle = Bundle()

    var node: LSSDPNodes? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vdf_setting_up_family_routines_layout)


        bundle = intent.extras!!
        if (bundle != null) {
            node = bundle.getSerializable("deviceDetails") as LSSDPNodes
        }



        btnProceed.setOnClickListener {
            val intent = Intent(this@VodafoneSettingUpFamilyRoutinesActivity, VodafoneAlexaDinnerTimeDeviceListActivity::class.java)
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