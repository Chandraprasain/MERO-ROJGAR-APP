package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class ParsedResumeProposal(
    val headline: String,
    val summary: String,
    val skills: List<String>,
    val experienceSummary: String,
    val educationSummary: String,
    val atsScore: Int,
    val improvementDiffs: List<String>
)

data class VoiceSearchFilters(
    val titleQuery: String,
    val district: String,
    val workMode: String,
    val minSalaryNpr: Long,
    val skills: List<String>
)

data class GeneratedJobDraft(
    val titleEn: String,
    val titleNe: String,
    val description: String,
    val suggestedSkills: List<String>
)

class AiLogicRepository {

    private companion object {
        const val TAG = "AiLogicRepository"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val TIMEOUT_MS = 60_000L
    }

    private val resumeSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string("Concise professional headline"),
            "summary" to Schema.string("Clean professional summary grounded only in provided info"),
            "skills" to Schema.array(Schema.string(), "Extracted skills"),
            "experienceSummary" to Schema.string("Formatted work experience grounded strictly in user input"),
            "educationSummary" to Schema.string("Formatted education grounded strictly in user input"),
            "atsScore" to Schema.integer("ATS readiness score from 0 to 100"),
            "improvementDiffs" to Schema.array(
                Schema.string(),
                "Actionable advisory suggestions for the user to approve or reject"
            )
        )
    )

    private val voiceIntentSchema = Schema.obj(
        properties = mapOf(
            "titleQuery" to Schema.string("Job title or role keywords extracted"),
            "district" to Schema.string("Nepal district or city such as Kathmandu, Lalitpur, Pokhara, Biratnagar, or empty if unspecified"),
            "workMode" to Schema.enumeration(listOf("any", "onsite", "hybrid", "remote"), "Work mode preference"),
            "minSalaryNpr" to Schema.integer("Minimum monthly salary in NPR, 0 if unspecified"),
            "skills" to Schema.array(Schema.string(), "Extracted skill keywords")
        )
    )

    private val jobDraftSchema = Schema.obj(
        properties = mapOf(
            "titleEn" to Schema.string("Clear English job title"),
            "titleNe" to Schema.string("Natural Nepali job title in Devanagari"),
            "description" to Schema.string("Inclusive, transparent job description with responsibilities and qualifications"),
            "suggestedSkills" to Schema.array(Schema.string(), "5 key skills for this role")
        )
    )

    private val resumeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = resumeSchema
            }
        )
    }

    private val voiceModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = voiceIntentSchema
            }
        )
    }

    private val jdModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = jobDraftSchema
            }
        )
    }

    private val coachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(
                    "You are Mero Rojgar's advisory Interview Coach for Nepal job seekers. " +
                        "Respond in the requested language (English or Nepali Devanagari). " +
                        "Give encouraging, structured feedback on clarity, technical accuracy, and concrete examples. " +
                        "Never automatically judge, reject, or rank out a candidate; keep guidance constructive and brief (under 140 words), then ask the next relevant interview question."
                )
            }
        )
    }

    suspend fun parseAndImproveResume(
        rawText: String,
        cvImage: Bitmap?,
        targetLanguage: String
    ): Result<ParsedResumeProposal> = try {
        val response = withTimeout(TIMEOUT_MS) {
            resumeModel.generateContent(
                content {
                    text(
                        "You are an ethical, advisory Resume Studio parser for Mero Rojgar Nepal. " +
                            "Target output language: $targetLanguage. " +
                            "STRICT RULE: Never invent experience, employers, degrees, or dates not present in the user's input. " +
                            "Extract fields cleanly, compute an honest ATS score (0-100), and provide 3 concrete advisory diff suggestions.\n\n" +
                            "User CV input:\n$rawText"
                    )
                    if (cvImage != null) {
                        image(cvImage)
                    }
                }
            )
        }
        val jsonText = response.text ?: error("Model returned an empty response.")
        val json = JSONObject(jsonText)
        val skillsArr = json.optJSONArray("skills")
        val diffsArr = json.optJSONArray("improvementDiffs")
        val skills = buildList {
            if (skillsArr != null) {
                for (i in 0 until skillsArr.length()) add(skillsArr.optString(i))
            }
        }.filter { it.isNotBlank() }
        val diffs = buildList {
            if (diffsArr != null) {
                for (i in 0 until diffsArr.length()) add(diffsArr.optString(i))
            }
        }.filter { it.isNotBlank() }

        Result.success(
            ParsedResumeProposal(
                headline = json.optString("headline"),
                summary = json.optString("summary"),
                skills = skills,
                experienceSummary = json.optString("experienceSummary"),
                educationSummary = json.optString("educationSummary"),
                atsScore = json.optInt("atsScore", 72).coerceIn(0, 100),
                improvementDiffs = diffs
            )
        )
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Resume parse timed out", e)
        Result.failure(Exception("Resume analysis timed out after 60s. Please retry."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Resume parse failed", e)
        Result.failure(e)
    }

    suspend fun parseVoiceSearchIntent(spokenOrTypedQuery: String): Result<VoiceSearchFilters> = try {
        val response = withTimeout(TIMEOUT_MS) {
            voiceModel.generateContent(
                "Parse this Nepali or English job search query into structured filters. " +
                    "Example: 'काठमाडौंमा ५० हजार तलब भएको सिभिल इन्जिनियर जागिर' -> titleQuery='Civil Engineer', district='Kathmandu', minSalaryNpr=50000.\n" +
                    "Query: $spokenOrTypedQuery"
            )
        }
        val jsonText = response.text ?: error("Model returned an empty response.")
        val json = JSONObject(jsonText)
        val skillsArr = json.optJSONArray("skills")
        val skills = buildList {
            if (skillsArr != null) {
                for (i in 0 until skillsArr.length()) add(skillsArr.optString(i))
            }
        }.filter { it.isNotBlank() }

        Result.success(
            VoiceSearchFilters(
                titleQuery = json.optString("titleQuery"),
                district = json.optString("district"),
                workMode = json.optString("workMode", "any"),
                minSalaryNpr = json.optLong("minSalaryNpr", 0L).coerceAtLeast(0L),
                skills = skills
            )
        )
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Voice intent timed out", e)
        Result.failure(Exception("Voice filter parsing timed out."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Voice intent failed", e)
        Result.failure(e)
    }

    suspend fun generateJobDescription(
        roleTitle: String,
        orgName: String,
        district: String,
        workMode: String
    ): Result<GeneratedJobDraft> = try {
        val response = withTimeout(TIMEOUT_MS) {
            jdModel.generateContent(
                "Draft an ethical, bias-free, transparent job posting for Nepal. " +
                    "Do NOT require candidate age, gender, caste, marital status, or photo. Do NOT ask for fees.\n" +
                    "Role: $roleTitle\nOrganization: $orgName\nDistrict: $district\nWork mode: $workMode"
            )
        }
        val jsonText = response.text ?: error("Model returned an empty response.")
        val json = JSONObject(jsonText)
        val skillsArr = json.optJSONArray("suggestedSkills")
        val skills = buildList {
            if (skillsArr != null) {
                for (i in 0 until skillsArr.length()) add(skillsArr.optString(i))
            }
        }.filter { it.isNotBlank() }

        Result.success(
            GeneratedJobDraft(
                titleEn = json.optString("titleEn", roleTitle),
                titleNe = json.optString("titleNe", roleTitle),
                description = json.optString("description"),
                suggestedSkills = skills
            )
        )
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "JD generation timed out", e)
        Result.failure(Exception("Job description generation timed out."))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "JD generation failed", e)
        Result.failure(e)
    }

    fun streamInterviewCoachReply(
        roleTitle: String,
        language: String,
        historyPairs: List<Pair<String, String>>,
        userMessage: String
    ): Flow<String> = flow {
        val historyContent = historyPairs.map { (speaker, msg) ->
            content(speaker) { text(msg) }
        }
        val chat = coachModel.startChat(history = historyContent)
        val prompt = "[Role: $roleTitle | Language: $language]\nCandidate says: $userMessage"
        val accumulated = StringBuilder()
        withTimeout(TIMEOUT_MS) {
            chat.sendMessageStream(prompt).collect { chunk ->
                val part = chunk.text.orEmpty()
                if (part.isNotEmpty()) {
                    accumulated.append(part)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            error("Interview Coach returned an empty response.")
        }
    }
}
