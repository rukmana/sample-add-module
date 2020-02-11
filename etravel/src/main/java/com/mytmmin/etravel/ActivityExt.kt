package com.mytmmin.etravel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

fun Context.navigatorImplicit(
    activityPackage: String,
    className: String,
    extras: Bundle = Bundle(),
    clearStack: Boolean = false,
    option: Bundle? = null) {
    val intent = Intent()
    try {
        intent.setClassName(activityPackage, className).putExtras(extras)

        if (clearStack) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

//        if (option != null) {
//            startActivity(intent, option)
//        } else {
        startActivity(intent)
//        }

    } catch (e: Exception) {
        Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }
}