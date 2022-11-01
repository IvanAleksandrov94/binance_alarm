package com.grapesapps.cryptochecker

import com.google.gson.annotations.SerializedName

data class PriceModel(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("price")
    val price: String
)
