package com.chargercompanion.aa

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.chargercompanion.R

class CategoryScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
            .addItem(row(PlaceKind.GAS))
            .addItem(row(PlaceKind.FOOD))
            .addItem(row(PlaceKind.PARKING))
            .build()

        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.aa_places_title))
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(list)
            .build()
    }

    private fun row(kind: PlaceKind): Row =
        Row.Builder()
            .setTitle(kind.label)
            .setBrowsable(true)
            .setOnClickListener {
                screenManager.push(PlacesListScreen(carContext, kind))
            }
            .build()
}
