package com.example.doubletap

import android.annotation.SuppressLint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

// 파일 처리를 위한 데이터 클래스
@SuppressLint("SimpleDateFormat")
data class Files(
    val file: File, // 파일 객체
    val fileDir: String = file.absolutePath, // 파일 절대 경로
    val name: String = if (file.extension == "md") file.nameWithoutExtension else "", // 파일 이름
    val lastEditDate: String = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(file.lastModified())) // 마지막 수정 날짜
)
