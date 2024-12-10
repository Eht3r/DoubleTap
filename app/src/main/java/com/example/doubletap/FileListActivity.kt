package com.example.doubletap

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doubletap.databinding.ActivityFileListBinding
import com.google.android.material.textfield.TextInputEditText
import java.io.File

class FileListActivity : AppCompatActivity() {
    private lateinit var rootDir: File
    private lateinit var rootFolderName: String
    private lateinit var binding: ActivityFileListBinding
    private lateinit var adapter: FileListAdapter

    private var folderCount: Int = 0
    private val items = mutableListOf<Files>()


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFileListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        folderCount =
            File(intent.getStringExtra("rootDir")!!).listFiles { folder -> folder.isDirectory }?.size
                ?: 0
        rootDir = intent.getStringExtra("folderDir")?.let { File(it) }!!
        rootFolderName = intent.getStringExtra("folderName")!!

        binding.folderLayout.folderName.visibility = View.GONE
        binding.folderLayout.fileCount.text = "${folderCount}개의 폴더"
        binding.folderLayout.root.setOnClickListener {
            finish()
        }

        binding.swipeLayout.setOnRefreshListener {
            loadFiles()
            binding.swipeLayout.isRefreshing = false
        }

        binding.folder.text = rootFolderName


        adapter = FileListAdapter(items) { file ->
            if (!file.file.exists()) {
                showAddFileDialog()
            } else {
                val editIntent = Intent(this, EditTextActivity::class.java)
                editIntent.putExtra("file", file.file.absolutePath)
                startActivity(editIntent)
            }
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addItemDecoration(FileListAdapterDecoration())
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // 상단바와 UI 요소 겹침 문제 해결
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemInsets.left,
                systemInsets.top,
                systemInsets.right,
                systemInsets.bottom
            )
            insets
        }

        loadFiles()

        // 스와이프 기능 추가
        val swipeController = SwipeController(object : SwipeControllerActions {
            override fun onLeftClicked(position: Int) {
                // 선택한 파일 가져오기
                val file = items[position].file
                // 압축 파일 공유
                Funbox().shareFile(this@FileListActivity, file)
            }

            override fun onRightClicked(position: Int) {
                adapter.deleteItem(position)
            }
        })
        val itemTouchHelper = ItemTouchHelper(swipeController)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadFiles() {
        val files =
            rootDir.listFiles { file -> file.isFile && file.extension == "md" }?.map { file ->
                Files(file)
            } ?: emptyList()

        val sortedFiles = files.sortedBy { it.name }

        // 발견된 파일들을 리스트에 추가
        items.clear()
        items.addAll(sortedFiles)

        // adapter에게 데이터 변경을 알림
        adapter.notifyDataSetChanged()
        Log.d("loadFiles", "Final items: ${items.map { it.name }}")
    }

    // 파일 추가 다이얼로그
    private fun showAddFileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edittext, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editText)
        editText.hint = "파일명을 입력하세요."
        val dialog = AlertDialog.Builder(
            this,
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog
        )
            .setTitle("새 파일 추가")
            .setMessage("파일 이름을 입력하세요")
            .setView(dialogView)
            .setPositiveButton("추가") { _, _ ->
                val fileName = editText.text.toString()
                if (fileName.isNotEmpty()) {
                    addNewFile(fileName)
                }
            }
            .setNegativeButton("취소", null)
            .create()
        dialog.show()
    }

    // 새로운 파일을 추가
    private fun addNewFile(fileName: String) {
        val newFile = File(rootDir, "$fileName.md")
        if (newFile.createNewFile() || newFile.exists()) {
            items.removeAll { it.name == fileName }
            val newItem = Files(newFile)
            items.add(items.size, newItem)
            adapter.notifyItemInserted(items.size - 1)
        } else {
            Log.e("addNewFile", "Failed to create file: $fileName")
        }
    }
}