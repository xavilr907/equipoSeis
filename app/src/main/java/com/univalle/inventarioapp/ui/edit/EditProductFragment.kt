package com.univalle.inventarioapp.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.univalle.inventarioapp.data.local.AppDatabase

class EditProductFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Placeholder minimal view
        val v = inflater.inflate(android.R.layout.simple_list_item_1, container, false)
        return v
    }
}

