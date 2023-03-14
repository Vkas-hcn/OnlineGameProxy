package com.vkas.onlinegameproxy.ui.list

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.vkas.onlinegameproxy.R
import com.vkas.onlinegameproxy.bean.OgVpnBean
import com.vkas.onlinegameproxy.key.Constant
import com.vkas.onlinegameproxy.utils.OnlineGameUtils.getFlagThroughCountryEc

class ListAdapter (data: MutableList<OgVpnBean>?) :
    BaseQuickAdapter<OgVpnBean, BaseViewHolder>(
        R.layout.item_service,
        data
    ) {

    override fun convert(holder: BaseViewHolder, item: OgVpnBean) {
        if (item.og_best == true) {
            holder.setText(R.id.txt_country, Constant.FASTER_OG_SERVER)
            holder.setImageResource(
                R.id.img_flag,
                getFlagThroughCountryEc(Constant.FASTER_OG_SERVER)
            )
        } else {
            holder.setText(R.id.txt_country, item.ongpro_country + "-" + item.ongpro_city)
            holder.setImageResource(
                R.id.img_flag,
                getFlagThroughCountryEc(item.ongpro_country.toString())
            )
        }
        if(holder.adapterPosition%2==0){
            holder.setBackgroundResource(R.id.con_item, R.drawable.bg_list_item)
        }else{
            holder.setBackgroundResource(R.id.con_item, R.color.transparent)
        }
        if (item.og_check == true) {
            holder.setBackgroundResource(R.id.img_chek, R.drawable.ic_item_chek)
        } else {
            holder.setBackgroundResource(R.id.img_chek, R.drawable.ic_item_dischek)
        }
    }
}