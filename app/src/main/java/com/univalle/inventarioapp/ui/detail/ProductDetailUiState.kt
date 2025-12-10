package com.univalle.inventarioapp.ui.detail

import com.univalle.inventarioapp.data.model.ProductEntity

data class ProductDetailUiState(
    val loading: Boolean = true,
    val product: ProductEntity? = null,
    val total: Double = 0.0,
    val error: String? = null
)
