package com.example.doubletap

import java.io.File

data class Folder(
    val folder: File,
    val folderDir: String = folder.absolutePath,
    val name: String = if (folder.path.isEmpty()) "폴더 추가" else folder.name,
    val fileCount: Int? = if (folder.path.isEmpty()) null else folder.listFiles { _, name -> name.endsWith(".md") }?.size
)
