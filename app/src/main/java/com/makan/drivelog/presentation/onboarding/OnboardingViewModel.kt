package com.makan.drivelog.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.drivelog.data.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    val prefs: PreferencesManager
) : ViewModel() {

    val mapSeen      = prefs.onboardingMapSeen
    val tripsSeen    = prefs.onboardingTripsSeen
    val navigateSeen = prefs.onboardingNavigateSeen
    val boardSeen    = prefs.onboardingBoardSeen
    val profileSeen  = prefs.onboardingProfileSeen

    fun markMapSeen()      { viewModelScope.launch { prefs.setOnboardingMapSeen() } }
    fun markTripsSeen()    { viewModelScope.launch { prefs.setOnboardingTripsSeen() } }
    fun markNavigateSeen() { viewModelScope.launch { prefs.setOnboardingNavigateSeen() } }
    fun markBoardSeen()    { viewModelScope.launch { prefs.setOnboardingBoardSeen() } }
    fun markProfileSeen()  { viewModelScope.launch { prefs.setOnboardingProfileSeen() } }
}
