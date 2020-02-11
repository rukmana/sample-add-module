package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.mytmmin.etravel.GitsRouteNavigation
import kotlinx.android.synthetic.main.activity_main.*
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intentButton.setOnClickListener {
//            intent = Intent(applicationContext, "com.mytmmin.etravel.Activity.MyTravelMainActivity" :: class.java)
//            startActivity(intent)
            try {
                GitsRouteNavigation.openMyTravel(this)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }

    }
}
