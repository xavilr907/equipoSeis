package com.univalle.inventarioapp.ui.detail

sealed class ProductDetailEvent {
    object NavigateBack : ProductDetailEvent()
    object NavigateToEdit : ProductDetailEvent()
}
