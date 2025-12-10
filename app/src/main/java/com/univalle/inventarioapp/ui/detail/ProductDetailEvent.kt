package com.univalle.inventarioapp.ui.detail

sealed class ProductDetailEvent {
    object NavigateBackToInventory : ProductDetailEvent()
    object NavigateToEditProduct : ProductDetailEvent()
}
