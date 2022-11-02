package com.grapesapps.cryptochecker.models

import com.google.gson.annotations.SerializedName

data class PriceModel(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("price")
    val price: String
)
