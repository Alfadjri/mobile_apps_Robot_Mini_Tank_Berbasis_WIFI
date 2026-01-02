package com.alfadjri28.e_witank.utils

import android.app.Activity
import android.content.pm.ActivityInfo

fun setLandscape(activity: Activity) {
    activity.requestedOrientation =
        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}

fun setPortrait(activity: Activity) {
    activity.requestedOrientation =
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}
