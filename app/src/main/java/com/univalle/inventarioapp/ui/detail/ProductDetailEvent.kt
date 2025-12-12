package com.univalle.inventarioapp.ui.detail

/**
 * Representa los eventos que pueden ocurrir en la pantalla de detalle
 * de un producto dentro de la aplicación.
 *
 * Esta sealed class permite manejar los eventos desde un ViewModel
 * usando un único flujo unificado, asegurando que cada evento esté
 * claramente definido y sea seguro en tiempo de compilación.
 *
 * Eventos disponibles:
 * - [NavigateBack]: indica que se debe regresar a la pantalla anterior.
 * - [NavigateToEdit]: indica la navegación hacia la pantalla de edición
 *   del producto actual.
 */
sealed class ProductDetailEvent {

    /**
     * Evento que solicita volver atrás en la navegación.
     */
    object NavigateBack : ProductDetailEvent()

    /**
     * Evento que solicita navegar hacia la pantalla de edición
     * del producto actualmente visualizado.
     */
    object NavigateToEdit : ProductDetailEvent()
}
