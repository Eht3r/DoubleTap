package com.example.doubletap

import android.annotation.SuppressLint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
data class Files(
    val file: File,
    val fileDir: String = file.absolutePath,
    val name: String = if (file.extension == "md") file.nameWithoutExtension else "",
    val lastEditDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(file.lastModified()))
)
