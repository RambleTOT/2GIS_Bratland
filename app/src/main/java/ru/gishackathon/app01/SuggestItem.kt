package ru.gishackathon.app01

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.gishackathon.app01.R

data class SuggestItem(
    val title: String,
    val subtitle: String?,
)

class SuggestAdapter(
    private val onClick: (SuggestItem) -> Unit
) : RecyclerView.Adapter<SuggestAdapter.VH>() {

    private val items = mutableListOf<SuggestItem>()

    fun submit(list: List<SuggestItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val icon: ImageView = v.findViewById(R.id.ivIcon)
        private val title: TextView = v.findViewById(R.id.tvTitle)
        private val sub: TextView = v.findViewById(R.id.tvSubtitle)
        fun bind(it: SuggestItem) {
            title.text = it.title
            sub.text = it.subtitle ?: ""
            sub.visibility = if (it.subtitle.isNullOrBlank()) View.GONE else View.VISIBLE
            itemView.setOnClickListener { onClick(items[bindingAdapterPosition]) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_suggest, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size
}
