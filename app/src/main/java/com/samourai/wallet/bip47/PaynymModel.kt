package com.samourai.wallet.bip47


data class PaynymModel(
    val code: String? = "",
    val nymId: String? = "",
    val nymName: String? = "",
    val segwit: Boolean? = false
)