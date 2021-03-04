package com.murgupluoglu.timetablelib

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ConvertUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition


data class RecyclerViewItem(val iconName: String, val name: String)
interface MyAdapterClickLister {
    fun onClicked(position: Int, textView: TextView)
}

class MyAdapter(private val myDataset: ArrayList<RecyclerViewItem>, val myAdapterClickListener: MyAdapterClickLister) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

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

        if (URLUtil.isValidUrl(item.iconName)) {
            Glide.with(context)
                    .asBitmap()
                    .load(item.iconName)
                    .into(object : SimpleTarget<Bitmap>(ConvertUtils.dp2px(24f), ConvertUtils.dp2px(24f)) {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val drawable = BitmapDrawable(context.resources, resource)
                            DrawableCompat.setTint(drawable, Color.WHITE)
                            holder.nameTextView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                        }
                    })
        } else {
            val resId = context.resources.getIdentifier(item.iconName, "drawable", context.packageName)
            val drawable = ContextCompat.getDrawable(context, resId)!!
            holder.nameTextView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = myDataset.size
}