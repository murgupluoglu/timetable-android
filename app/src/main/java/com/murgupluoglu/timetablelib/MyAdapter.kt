package com.murgupluoglu.timetablelib

import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView


data class RecyclerViewItem(val iconName : String, val name : String)
interface MyAdapterClickLister{
    fun onClicked(position: Int, textView: TextView)
}
class MyAdapter(private val myDataset: ArrayList<RecyclerViewItem>, val myAdapterClickListener : MyAdapterClickLister) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var nameTextView: TextView = view.findViewById<View>(R.id.nameTextView) as TextView
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.MyViewHolder {

        val layout = LayoutInflater.from(parent.context).inflate(R.layout.button_layout, parent, false)

        return MyViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        //holder.iconImageView
        val item = myDataset[position]
        holder.nameTextView.text = item.name
        holder.itemView.setOnLongClickListener {
            myAdapterClickListener.onClicked(position, holder.nameTextView)
            true
        }


        val context = holder.itemView.context
        val resId = context.resources.getIdentifier(item.iconName, "drawable", context.packageName)
        val drawable = ContextCompat.getDrawable(holder.itemView.context, resId)!!
        holder.nameTextView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}