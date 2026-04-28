package com.whc06.trainer.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whc06.trainer.training.CommonMvc
import com.whc06.trainer.training.Hand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "whc06_prefs")

object Prefs {
    private val MVC_BILATERAL = doublePreferencesKey("mvc_bilateral")
    private val MVC_LEFT = doublePreferencesKey("mvc_left")
    private val MVC_RIGHT = doublePreferencesKey("mvc_right")
    private val MVC_SOURCE = stringPreferencesKey("mvc_source")
    private val MVC_UPDATED = longPreferencesKey("mvc_updated")
    private val SELECTED_HAND = stringPreferencesKey("selected_hand")
    private val TARE_KG = doublePreferencesKey("tare_kg")
    private val TARGET_PCT = intPreferencesKey("target_pct")
    private val ZONE_TOL = intPreferencesKey("zone_tol")
    private val STABLE_ONLY = booleanPreferencesKey("stable_only")
    private val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    private val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    private val GRIP_TYPE = stringPreferencesKey("grip_type")
    private val CF_TUTORIAL_SEEN = booleanPreferencesKey("cf_tutorial_seen")
    private val UNITS_KG = booleanPreferencesKey("units_kg")
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val ONBOARDED = booleanPreferencesKey("onboarded")

    fun observe(ctx: Context): Flow<PrefsState> =
        ctx.dataStore.data.map { it.toState() }

    suspend fun setMvc(ctx: Context, mvc: CommonMvc) {
        ctx.dataStore.edit { p ->
            p[MVC_BILATERAL] = mvc.bilateralKg
            p[MVC_LEFT] = mvc.leftKg
            p[MVC_RIGHT] = mvc.rightKg
            p[MVC_SOURCE] = mvc.source.name
            p[MVC_UPDATED] = mvc.updatedAt
        }
    }

    suspend fun setHand(ctx: Context, hand: Hand) {
        ctx.dataStore.edit { it[SELECTED_HAND] = hand.name }
    }

    suspend fun setTare(ctx: Context, kg: Double) {
        ctx.dataStore.edit { it[TARE_KG] = kg }
    }

    suspend fun setTargetPct(ctx: Context, pct: Int) {
        ctx.dataStore.edit { it[TARGET_PCT] = pct }
    }

    suspend fun setZoneTolerance(ctx: Context, pct: Int) {
        ctx.dataStore.edit { it[ZONE_TOL] = pct }
    }

    suspend fun setStableOnly(ctx: Context, on: Boolean) {
        ctx.dataStore.edit { it[STABLE_ONLY] = on }
    }

    suspend fun setTtsEnabled(ctx: Context, on: Boolean) {
        ctx.dataStore.edit { it[TTS_ENABLED] = on }
    }

    suspend fun setHapticEnabled(ctx: Context, on: Boolean) {
        ctx.dataStore.edit { it[HAPTIC_ENABLED] = on }
    }

    suspend fun setGripType(ctx: Context, grip: com.whc06.trainer.training.GripType) {
        ctx.dataStore.edit { it[GRIP_TYPE] = grip.name }
    }

    suspend fun setCfTutorialSeen(ctx: Context, seen: Boolean) {
        ctx.dataStore.edit { it[CF_TUTORIAL_SEEN] = seen }
    }

    suspend fun setUnitsKg(ctx: Context, kg: Boolean) {
        ctx.dataStore.edit { it[UNITS_KG] = kg }
    }

    suspend fun setThemeMode(ctx: Context, mode: ThemeMode) {
        ctx.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setOnboarded(ctx: Context, seen: Boolean) {
        ctx.dataStore.edit { it[ONBOARDED] = seen }
    }

    private fun Preferences.toState(): PrefsState = PrefsState(
        mvc = CommonMvc(
            bilateralKg = this[MVC_BILATERAL] ?: 0.0,
            leftKg = this[MVC_LEFT] ?: 0.0,
            rightKg = this[MVC_RIGHT] ?: 0.0,
            source = runCatching {
                CommonMvc.Source.valueOf(this[MVC_SOURCE] ?: "MANUAL")
            }.getOrElse { CommonMvc.Source.MANUAL },
            updatedAt = this[MVC_UPDATED] ?: 0L
        ),
        hand = runCatching { Hand.valueOf(this[SELECTED_HAND] ?: "BOTH") }.getOrElse { Hand.BOTH },
        tareKg = this[TARE_KG] ?: 0.0,
        targetPct = this[TARGET_PCT] ?: 70,
        zoneTolPct = this[ZONE_TOL] ?: 5,
        stableOnly = this[STABLE_ONLY] ?: false,
        ttsEnabled = this[TTS_ENABLED] ?: true,
        hapticEnabled = this[HAPTIC_ENABLED] ?: true,
        gripType = runCatching {
            com.whc06.trainer.training.GripType.valueOf(this[GRIP_TYPE] ?: "HALF_CRIMP")
        }.getOrElse { com.whc06.trainer.training.GripType.HALF_CRIMP },
        cfTutorialSeen = this[CF_TUTORIAL_SEEN] ?: false,
        unitsKg = this[UNITS_KG] ?: true,
        themeMode = runCatching { ThemeMode.valueOf(this[THEME_MODE] ?: "SYSTEM") }
            .getOrElse { ThemeMode.SYSTEM },
        onboarded = this[ONBOARDED] ?: false
    )
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class PrefsState(
    val mvc: CommonMvc,
    val hand: Hand,
    val tareKg: Double,
    val targetPct: Int,
    val zoneTolPct: Int,
    val stableOnly: Boolean,
    val ttsEnabled: Boolean,
    val hapticEnabled: Boolean,
    val gripType: com.whc06.trainer.training.GripType,
    val cfTutorialSeen: Boolean,
    val unitsKg: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false
)
