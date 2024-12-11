package com.example.doubletap

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.toSpannable
import com.example.doubletap.databinding.ActivityEditTextBinding
import io.noties.markwon.Markwon
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import java.io.File

class EditTextActivity : AppCompatActivity() {
    private lateinit var markwon: Markwon
    private lateinit var binding: ActivityEditTextBinding

    private var isFirstRun = false
    private var lastChar: Char? = null
    private var repeatCount = 0
    private val markdownStack = ArrayDeque<String>()
    private var guideCount = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        markwon = Markwon.create(this)

        val editText = binding.editText
        val textView = binding.textView

        // fileListActivity에서 파일 경로 받아오기
        val intent = intent
        val file = intent.getStringExtra("file")?.let { File(it) }

        markwon = Markwon.builder(this)
            .usePlugin(MarkwonInlineParserPlugin.create()) // 인라인 파서 플러그인 추가
            .usePlugin(JLatexMathPlugin.create(textView.textSize) { builder ->
                builder.inlinesEnabled(true) // 인라인 수식 활성화
            })
            .usePlugin(TablePlugin.create(this))
            .usePlugin(StrikethroughPlugin.create())
            .build()

        // 파일 내용 불러오기
        val content = file?.let { loadFile(it) }
        editText.setText(content)

        // 입력된 Markdown 렌더링
        markwon.setMarkdown(textView, content ?: "")

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s?.toString() ?: ""
                if (inputText.isNotEmpty()) {
                    val currentChar = inputText.last()

                    if (currentChar == ' ') {
                        // 스페이스바 처리
                        if (lastChar == ' ') {
                            repeatCount++
                            if (repeatCount == 1) {
                                applyMarkdownFromStack(editText)
                                lastChar = null // 스페이스 입력 후 초기화
                                repeatCount = 0
                            }
                        } else {
                            lastChar = ' '
                            repeatCount = 0
                        }
                    } else if (currentChar in listOf('B', 'I', 'S', 'H', 'T', 'L', 'C', 'M', 'D')) {
                        // 특정 키 처리
                        if (currentChar == lastChar) {
                            repeatCount++
                            if (repeatCount == 1) {
                                handleDoubleTap(currentChar, editText)
                                lastChar = null // 특정 키 입력 후 초기화
                                repeatCount = 0
                            }
                        } else {
                            lastChar = currentChar
                            repeatCount = 0
                        }
                    } else {
                        // 처리되지 않은 문자 입력 시 초기화
                        lastChar = null
                        repeatCount = 0
                    }
                }
                markwon.setMarkdown(textView, inputText) // Markdown 렌더링

                val currentText = textView.text.toSpannable()
                val lastMarkdown = if (markdownStack.isNotEmpty()) markdownStack.last() else ""
                guideCount = lastMarkdown.length

                val spannableStringBuilder = SpannableStringBuilder(lastMarkdown)

                val startIndex = 0
                val endIndex = guideCount

                spannableStringBuilder.setSpan(
                    ForegroundColorSpan(Color.GRAY),
                    startIndex,
                    endIndex,
                    0
                )

                textView.text = SpannableStringBuilder(currentText).append(spannableStringBuilder)
            }
        })

        // 5분 마다 저장하기
        val autoSave = object : Runnable {
            override fun run() {
                if (file != null && !isFirstRun) {
                    saveFile(file, editText.text.toString())
                }
                editText.postDelayed(this, 300000)
            }
        }

        // 뒤로 가기 등의 방법으로 액티비티 종료시 저장
        onBackPressedDispatcher.addCallback(this) {
            if (file != null) {
                saveFile(file, editText.text.toString())
            }
            finish()
        }

        editText.post(autoSave)
    }

    private fun handleDoubleTap(char: Char, editText: EditText) {
        val currentText = editText.text.toString()
        val updatedText = currentText.dropLast(2) // 입력한 문자 제거
        editText.setText(updatedText)
        editText.setSelection(updatedText.length)

        when (char) {
            'B' -> addMarkdownSyntax(editText, "**", "**") // Bold
            'C' -> addMarkdownSyntax(editText, "`", "`") // Code block
            'D' -> addMarkdownSyntax(editText, "[", ")", "](") // Link
            'H' -> addMarkdownSyntax(editText, "# ") // Header
            'I' -> addMarkdownSyntax(editText, "*", "*") // Italic
            'L' -> addMarkdownSyntax(editText, "---") // Line divider
            'M' -> addMarkdownSyntax(editText, "$\n", "\n$") // Math block
            'S' -> addMarkdownSyntax(editText, "~~", "~~") // Strikethrough
            'T' -> createDynamicTable(editText) // Table
        }
    }

    private fun createDynamicTable(editText: EditText) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_table_input, null)
        val rowInput = dialogView.findViewById<EditText>(R.id.rowInput)
        val colInput = dialogView.findViewById<EditText>(R.id.colInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Table Dimensions")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                val row = rowInput.text.toString().toIntOrNull() ?: 0
                val col = colInput.text.toString().toIntOrNull() ?: 0
                if (row > 0 && col > 0) {
                    generateTableMarkdown(editText, row, col)
                } else {
                    Toast.makeText(this, "Please enter valid numbers!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun generateTableMarkdown(editText: EditText, row: Int, col: Int) {
        // 헤더 생성
        val colDivider = "|"
        val colDividers = Array(col) {colDivider}
        val headerDividers = Array(col) {colDivider}
        colDividers[0] = colDividers.last() + "\n"

        val divider = (0..col).joinToString("|") { "---" } + "\n"
        headerDividers[0] = colDividers.last() + "\n" + divider

        // 테이블 문법 추가
        for (i in 0 until row) {
            addMarkdownSyntax(editText, "", *colDividers)
        }
        addMarkdownSyntax(editText, colDivider, *headerDividers)
    }

    private fun addMarkdownSyntax(editText: EditText, vararg syntax: String) {
        markdownStack.addAll(syntax.sliceArray(1 until syntax.size)) // 스택에 추가

        val currentText = editText.text.toString()
        val updatedText = currentText + syntax[0]
        editText.setText(updatedText)
        editText.setSelection(updatedText.length)
    }

    private fun addMarkdownSyntax(editText: EditText, syntax: String) {
        val currentText = editText.text.toString()
        val updatedText = currentText + syntax
        editText.setText(updatedText)
        editText.setSelection(updatedText.length) // 커서를 끝으로 이동
    }

    private fun applyMarkdownFromStack(editText: EditText) {
        if (markdownStack.isNotEmpty()) {
            val markdownSyntax = markdownStack.removeLast() // 스택에서 하나 pop
            val currentText = editText.text.toString().dropLast(2) // 마지막 공백 제거
            val updatedText = currentText + markdownSyntax
            editText.setText(updatedText)
            editText.setSelection(updatedText.length) // 커서를 끝으로 이동
        }
    }

    private fun updateStackPreview(stackPreview: TextView) {
        val previewText = if (markdownStack.isNotEmpty()) {
            "Next Markdown: " + markdownStack.joinToString(", ")
        } else {
            "No Markdown in stack."
        }
        stackPreview.text = previewText
    }

    // editText에 작성된 내용 파일로 저장
    private fun saveFile(file: File, content: String) {
        Log.d("EditTextActivity", "saveFile 호출됨")
        try {
            file.writeText(content)
            Toast.makeText(this, "저장 되었습니다.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("EditTextActivity", "파일 저장 실패: ${e.message}")
            Toast.makeText(this, "파일 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 파일 내용 불러오기
    private fun loadFile(file: File): String {
        Log.d("EditTextActivity", "loadFile 호출됨")
        isFirstRun = true
        return try {
            if (file.exists()) {
                file.readText()
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("EditTextActivity", "파일 불러오기 실패: ${e.message}")
            Toast.makeText(this, "파일 불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            ""
        }
    }
}