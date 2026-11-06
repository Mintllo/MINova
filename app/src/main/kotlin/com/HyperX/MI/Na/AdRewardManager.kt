package com.HyperX.MI.Na

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdRewardManager {
    private var rewardedAd: RewardedAd? = null
    private var adLoadFailCount = 0
    private val maxRetry = 3
    private val adUnitId = "YOUR_ADMOB_UNIT_ID"

    fun loadAd() {
        if (adLoadFailCount >= maxRetry) return
        RewardedAd.load(App.instance, adUnitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                adLoadFailCount = 0
                Log.d("AdReward", "广告加载成功")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
                adLoadFailCount++
                Log.d("AdReward", "广告加载失败: code=${error.code}, msg=${error.message}")
            }
        })
    }

    fun showAd(activity: Activity, onReward: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { onReward() }
            rewardedAd = null
        }
    }

    fun isAdReady(): Boolean = rewardedAd != null
}