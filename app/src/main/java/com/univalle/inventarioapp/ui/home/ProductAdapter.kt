package com.univalle.inventarioapp.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.univalle.inventarioapp.data.model.ProductEntity
import com.univalle.inventarioapp.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter :
    ListAdapter<ProductEntity, ProductAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProductEntity>() {
            override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity) =
                oldItem.code == newItem.code
            override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity) =
                oldItem == newItem
        }
    }

    inner class VH(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ProductEntity) {
            binding.tvName.text = item.name
            binding.tvCode.text = "Código: ${item.code}"

            // priceCents -> formatear como moneda simple
            val nf = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
            val priceCOP = item.priceCents / 100.0
            binding.tvPrice.text = "Precio: ${nf.format(priceCOP)}"

            binding.tvQty.text = "Cantidad: ${item.quantity}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inf = LayoutInflater.from(parent.context)
        return VH(ItemProductBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}

