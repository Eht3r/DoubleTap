package com.example.doubletap

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.doubletap.databinding.FileAddBinding // file_add.xml
import com.example.doubletap.databinding.FileItemBinding // file_item.xml
import androidx.recyclerview.widget.RecyclerView
import java.io.File


class FileListAdapter(
    private val items: MutableList<Files>,
    private val itemClickListener: (Files) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 뷰홀더 클래스 정의
    class AddViewHolder(private val fileAddBinding: FileAddBinding) : RecyclerView.ViewHolder(fileAddBinding.root)
    class ItemViewHolder(val fileItemBinding: FileItemBinding) :
        RecyclerView.ViewHolder(fileItemBinding.root)

    // 뷰 타입을 정의하는 상수
    // 각가 다른 두개의 뷰 홀더를 사용하기 위함
    companion object {
        private const val VIEW_TYPE_ADD = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    // 뷰 타입을 반환하는 메서드
    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ADD else VIEW_TYPE_ITEM
    }

    // 뷰홀더 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_ADD -> {
                val binding = FileAddBinding.inflate(inflater, parent, false)
                AddViewHolder(binding)
            }

            VIEW_TYPE_ITEM -> {
                val binding = FileItemBinding.inflate(inflater, parent, false)
                ItemViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    // 뷰홀더에 데이터를 바인딩
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AddViewHolder -> {
                holder.itemView.setOnClickListener {
                    itemClickListener(Files(File("")))
                }
            }

            is ItemViewHolder -> {
                val item = items[position - 1]
                Log.d("Position", position.toString())
                holder.fileItemBinding.fileName.text = item.name
                holder.fileItemBinding.fileDate.text = item.lastEditDate
                holder.itemView.setOnClickListener {
                    itemClickListener(item)
                }
            }
        }
    }

    // 아이템의 개수 반환
    override fun getItemCount(): Int = items.size + 1

    // 아이템 삭제
    fun deleteItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)
    }
}