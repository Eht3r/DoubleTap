package com.example.doubletap

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.doubletap.databinding.FileAddBinding
import com.example.doubletap.databinding.FileItemBinding
import androidx.recyclerview.widget.RecyclerView
import java.io.File


class FileListAdapter(
    private val items: MutableList<Files>,
    private val itemClickListener: (Files) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class AddViewHolder(private val fileAddBinding: FileAddBinding) : RecyclerView.ViewHolder(fileAddBinding.root)
    class ItemViewHolder(val fileItemBinding: FileItemBinding) :
        RecyclerView.ViewHolder(fileItemBinding.root)

    companion object {
        private const val VIEW_TYPE_ADD = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ADD else VIEW_TYPE_ITEM
    }

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

    override fun getItemCount(): Int = items.size + 1

    fun deleteItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, itemCount)
    }
}