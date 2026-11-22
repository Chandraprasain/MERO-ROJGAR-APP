package com.example.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiLogicRepository
import com.example.data.GeneratedJobDraft
import com.example.data.ParsedResumeProposal
import com.example.data.VoiceSearchFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AiCallState<out T> {
    data object Idle : AiCallState<Nothing>
    data object Loading : AiCallState<Nothing>
    data class Streaming(val text: String) : AiCallState<Nothing>
    data class Success<T>(val data: T) : AiCallState<T>
    data class Error(val message: String) : AiCallState<Nothing>
}

data class CoachTurn(
    val speaker: String, // "user" or "model"
    val text: String
)

class AiStudioViewModel(
    private val aiRepository: AiLogicRepository = AiLogicRepository()
) : ViewModel() {

    private companion object {
        const val TAG = "AiStudioViewModel"
    }

    private val _resumeParseState = MutableStateFlow<AiCallState<ParsedResumeProposal>>(AiCallState.Idle)
    val resumeParseState: StateFlow<AiCallState<ParsedResumeProposal>> = _resumeParseState.asStateFlow()

    private val _voiceIntentState = MutableStateFlow<AiCallState<VoiceSearchFilters>>(AiCallState.Idle)
    val voiceIntentState: StateFlow<AiCallState<VoiceSearchFilters>> = _voiceIntentState.asStateFlow()

    private val _jdDraftState = MutableStateFlow<AiCallState<GeneratedJobDraft>>(AiCallState.Idle)
    val jdDraftState: StateFlow<AiCallState<GeneratedJobDraft>> = _jdDraftState.asStateFlow()

    private val _coachTurns = MutableStateFlow<List<CoachTurn>>(emptyList())
    val coachTurns: StateFlow<List<CoachTurn>> = _coachTurns.asStateFlow()

    private val _coachStreamState = MutableStateFlow<AiCallState<String>>(AiCallState.Idle)
    val coachStreamState: StateFlow<AiCallState<String>> = _coachStreamState.asStateFlow()

    fun analyzeResume(rawText: String, cvImage: Bitmap?, targetLanguage: String) {
        viewModelScope.launch {
            _resumeParseState.value = AiCallState.Loading
            val res = aiRepository.parseAndImproveResume(rawText, cvImage, targetLanguage)
            _resumeParseState.value = res.fold(
                onSuccess = { AiCallState.Success(it) },
                onFailure = { AiCallState.Error(it.localizedMessage ?: "AI Resume parse failed") }
            )
        }
    }

    fun resetResumeProposal() {
        _resumeParseState.value = AiCallState.Idle
    }

    fun parseVoiceQuery(query: String) {
        viewModelScope.launch {
            _voiceIntentState.value = AiCallState.Loading
            val res = aiRepository.parseVoiceSearchIntent(query)
            _voiceIntentState.value = res.fold(
                onSuccess = { AiCallState.Success(it) },
                onFailure = { AiCallState.Error(it.localizedMessage ?: "Voice parsing failed") }
            )
        }
    }

    fun clearVoiceIntent() {
        _voiceIntentState.value = AiCallState.Idle
    }

    fun generateJobDraft(roleTitle: String, orgName: String, district: String, workMode: String) {
        viewModelScope.launch {
            _jdDraftState.value = AiCallState.Loading
            val res = aiRepository.generateJobDescription(roleTitle, orgName, district, workMode)
            _jdDraftState.value = res.fold(
                onSuccess = { AiCallState.Success(it) },
                onFailure = { AiCallState.Error(it.localizedMessage ?: "JD draft failed") }
            )
        }
    }

    fun resetJobDraft() {
        _jdDraftState.value = AiCallState.Idle
    }

    fun sendInterviewMessage(roleTitle: String, language: String, userMessage: String) {
        val currentHistory = _coachTurns.value.map { it.speaker to it.text }
        _coachTurns.value = _coachTurns.value + CoachTurn("user", userMessage)
        viewModelScope.launch {
            _coachStreamState.value = AiCallState.Loading
            var finalReply = ""
            try {
                aiRepository.streamInterviewCoachReply(roleTitle, language, currentHistory, userMessage)
                    .collect { partial ->
                        finalReply = partial
                        _coachStreamState.value = AiCallState.Streaming(partial)
                    }
                _coachTurns.value = _coachTurns.value + CoachTurn("model", finalReply)
                _coachStreamState.value = AiCallState.Success(finalReply)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Interview coach timed out", e)
                _coachStreamState.value = AiCallState.Error("Interview coach timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Interview coach failed", e)
                _coachStreamState.value = AiCallState.Error(e.localizedMessage ?: "Coach reply failed.")
            }
        }
    }

    fun resetCoachSession() {
        _coachTurns.value = emptyList()
        _coachStreamState.value = AiCallState.Idle
    }
}
