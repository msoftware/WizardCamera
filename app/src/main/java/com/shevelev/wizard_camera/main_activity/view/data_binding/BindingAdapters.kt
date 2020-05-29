package com.shevelev.wizard_camera.main_activity.view.data_binding

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import com.shevelev.wizard_camera.main_activity.dto.ButtonState
import com.shevelev.wizard_camera.main_activity.dto.FilterListStartData
import com.shevelev.wizard_camera.main_activity.view.widgets.TitleWidget
import com.shevelev.wizard_camera.main_activity.view.widgets.filters_carousel.FiltersRecyclerView

@BindingAdapter("button_state")
fun setImageViewIsSelected(view: ImageView, valueToBind: LiveData<ButtonState>?) =
    valueToBind?.value?.let {
        when(it) {
            ButtonState.DISABLED -> {
                view.isSelected = false
                view.isEnabled = false
            }
            ButtonState.SELECTED -> {
                view.isSelected = true
                view.isEnabled = true
            }
            ButtonState.ACTIVE -> {
                view.isSelected = false
                view.isEnabled = true
            }
        }
    }

@BindingAdapter("title_text")
fun setTitleText(view: TitleWidget, valueToBind: LiveData<String>?) =
    valueToBind?.value?.let {
        view.show(it)
    }

@BindingAdapter("filters_data")
fun setFiltersData(view: FiltersRecyclerView, valueToBind: LiveData<FilterListStartData>?) =
    valueToBind?.value?.let {
        view.setStartData(it)
    }