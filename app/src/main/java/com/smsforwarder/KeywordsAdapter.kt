package com.smsforwarder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class KeywordsAdapter(
    private val keywords: MutableList<String>,
    private val onRemove: (String) -> Unit
) : RecyclerView.Adapter<KeywordsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textKeyword: TextView = view.findViewById(R.id.textKeyword)
        val buttonRemove: ImageButton = view.findViewById(R.id.buttonRemoveKeyword)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_keyword, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val keyword = keywords[position]
        holder.textKeyword.text = keyword
        holder.buttonRemove.setOnClickListener { onRemove(keyword) }
    }

    override fun getItemCount(): Int = keywords.size
}
