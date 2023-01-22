package com.charityright.charityauthority.auditor.model.DraftResponse

data class Nutrition(
    val create_at: String,
    val food_name: String,
    val form_id: String,
    val id: String,
    val percent: String,
    val updated_at: String
)