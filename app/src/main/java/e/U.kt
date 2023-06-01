package e

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import op.asd.R
import op.asd.bean.OgVpnBean
import op.asd.key.Constant
import op.asd.utils.OnlineGameUtils.getFlagThroughCountryEc
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import android.view.LayoutInflater
import android.view.ViewGroup


class U(private val dataList: MutableList<OgVpnBean>) :
    RecyclerView.Adapter<U.ViewHolder>() {

   inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtCountry: TextView = itemView.findViewById(R.id.txt_country)
        var imgFlag: ImageView = itemView.findViewById(R.id.img_flag)
        var conItem: ConstraintLayout = itemView.findViewById(R.id.con_item)
        var imgChek: ImageView = itemView.findViewById(R.id.img_chek)
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // 处理 item 点击事件
                    onItemClick(position)
                }
            }
        }
    }
    // 定义点击事件的回调接口
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    private var onItemClickListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    // 在 item 点击事件中触发回调
    private fun onItemClick(position: Int) {
        onItemClickListener?.onItemClick(position)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context: Context = parent.context
        val inflater = LayoutInflater.from(context)

        // 加载自定义的布局文件
        val itemView: View = inflater.inflate(R.layout.item_service, parent, false)

        // 创建ViewHolder对象
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 获取数据
        val item = dataList[position]
        // 将数据绑定到视图上
        if (item.og_best == true) {
            holder.txtCountry.text = Constant.FASTER_OG_SERVER
            holder.imgFlag.setImageResource(getFlagThroughCountryEc(Constant.FASTER_OG_SERVER))
        } else {

            holder.txtCountry.text = String.format(item.ongpro_country + "-" + item.ongpro_city)
            holder.imgFlag.setImageResource(getFlagThroughCountryEc(item.ongpro_country.toString()))
        }
        if (position % 2 == 0) {
            holder.conItem.setBackgroundResource(R.drawable.bg_list_item)
        } else {
            holder.conItem.setBackgroundResource(R.color.transparent)
        }
        if (item.og_check == true) {
            holder.imgChek.setBackgroundResource(R.drawable.ic_item_chek)
        } else {
            holder.imgChek.setBackgroundResource(R.drawable.ic_item_dischek)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }
    fun addData(newData: MutableList<OgVpnBean>) {
        dataList.addAll(newData)
        notifyDataSetChanged()
    }
}