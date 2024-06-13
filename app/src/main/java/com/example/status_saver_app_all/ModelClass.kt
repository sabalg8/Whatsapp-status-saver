package com.example.status_saver_app_all

import android.net.Uri

data class ModelClass(
    var path: String = "",
    var filename: String = "",
    var uri: Uri? = null
)