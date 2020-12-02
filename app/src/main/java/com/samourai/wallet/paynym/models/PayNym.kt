package com.samourai.wallet.paynym.models

/**
 * samourai-wallet-android
 */


data class PayNym(val code: String,
                  val nymName: String?,
                  val claimed: Boolean? = false,
                  val segwit: Boolean = false,
                  val nymId: String?)

data class NymResponse(
        val codes: ArrayList<PayNym>?,
        val followers: ArrayList<PayNym>?,
        val following: ArrayList<PayNym>?,
        val nymAvatar: String,
        val nymID: String,
        val nymName: String,
        val segwit: Boolean?,
)