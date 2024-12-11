package com.example.doubletap

import java.io.File

data class Folder(
    val folder: File, // 폴더 객체
    val folderDir: String = folder.absolutePath, // 폴더 절대 경로
    val name: String = if (folder.path.isEmpty()) "폴더 추가" else folder.name, // 폴더 이름
    val fileCount: Int? = if (folder.path.isEmpty()) null else folder.listFiles { _, name -> name.endsWith(".md") }?.size // 파일 개수
)
