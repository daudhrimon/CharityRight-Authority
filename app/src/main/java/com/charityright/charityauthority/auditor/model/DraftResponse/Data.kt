package com.charityright.charityauthority.auditor.model.DraftResponse

data class Data(
    val class_list: List<Class>,
    val compliment_list: List<Compliment>,
    val edu_type: String,
    val food_list: List<Food>,
    val image_list: List<Image>,
    val nutrition_list: List<Nutrition>,
    val site_visit_report: SiteVisitReport,
    val total_student: Int
)