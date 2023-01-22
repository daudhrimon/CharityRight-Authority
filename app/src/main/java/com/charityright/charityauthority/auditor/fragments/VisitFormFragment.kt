package com.charityright.charityauthority.auditor.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.charityright.charityauditor.Adapters.FoundationAdapter
import com.charityright.charityauditor.Adapters.NutritionAdapter
import com.charityright.charityauthority.R
import com.charityright.charityauthority.adapters.FormAddClassAdapter
import com.charityright.charityauthority.auditor.adapter.FoodAdapter
import com.charityright.charityauthority.auditor.adapter.ImageCategoryAdapter
import com.charityright.charityauthority.auditor.model.ComplaintsModel
import com.charityright.charityauthority.auditor.model.FieldImageModel
import com.charityright.charityauthority.auditor.model.FormClassModel
import com.charityright.charityauthority.auditor.model.NutritionModel
import com.charityright.charityauthority.auditor.viewModel.AuditorActivityViewModel
import com.charityright.charityauthority.auditor.viewModel.FieldVisitViewModel
import com.charityright.charityauthority.auditor.viewModel.ReportsViewModel
import com.charityright.charityauthority.databinding.FragmentVisitFromBinding
import com.charityright.charityauthority.model.FoodlistItem
import com.charityright.charityauthority.util.CustomUploadDialog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import androidx.lifecycle.Observer
import com.charityright.charityauthority.retrofit.BaseUrl
import kotlin.Exception

class VisitFormFragment : Fragment() {
    private var _binding: FragmentVisitFromBinding? = null
    private val binding get() = _binding!!

    private lateinit var nutritionLayoutManager: LinearLayoutManager
    private lateinit var nutritionAdapter: NutritionAdapter
    private lateinit var foodAdapter: FoodAdapter

    private lateinit var foundationLayoutManager: LinearLayoutManager
    private lateinit var foundationAdapter: FoundationAdapter

    private lateinit var imageCategoryManager: LinearLayoutManager
    private lateinit var imageCategoryAdapter: ImageCategoryAdapter

    private lateinit var addClassLayoutManager: LinearLayoutManager
    private lateinit var formAddClassAdapter: FormAddClassAdapter

    private lateinit var activityViewModel: AuditorActivityViewModel
    private lateinit var fieldVisitViewModel: FieldVisitViewModel

    private lateinit var reportsViewModel: ReportsViewModel

    private var school_id = ""
    private var as_time = ""

    private var schoolIdList = ArrayList<String>()
    private var schoolNameList = ArrayList<String>()
    private var schoolZoneIdList = ArrayList<String>()
    private var schoolTypeList = ArrayList<String>()
    private var schoolTotalStdList = ArrayList<String>()
    private var foodlistItem = ArrayList<FoodlistItem>()
    private var flag: Boolean = false

    private var mealQuality = ""
    private var foodItem = ""
    private var imageCategory = ""

    private var itemCount = 0
    private var save = false

    private var cacheFile: File? = null
    private var cacheFileUri: Uri? = null

    private var nutritionArrayList: ArrayList<NutritionModel> = ArrayList()
    private var complaintsArrayList: ArrayList<ComplaintsModel> = ArrayList()
    private var classArrayList: ArrayList<FormClassModel> = ArrayList()
    private var imageArrayList: ArrayList<FieldImageModel> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVisitFromBinding.inflate(inflater, container, false)

        activityViewModel =
            ViewModelProvider(requireActivity()).get(AuditorActivityViewModel::class.java)
        fieldVisitViewModel = ViewModelProvider(this).get(FieldVisitViewModel::class.java)

        reportsViewModel = ViewModelProvider(this).get(ReportsViewModel::class.java)
        CustomUploadDialog.init(requireContext())

        try {
            fieldVisitViewModel.draft_id = (arguments?.getString("draft_id", "")?:"")
            school_id = (arguments?.getString("id", "")?:"")
            fieldVisitViewModel.task_id = (arguments?.getString("task", "")?:"")
            Log.wtf("TaskID", fieldVisitViewModel.task_id)
            as_time = (arguments?.getString("as_time", "")?:"")
        } catch (e: Exception) {
            Log.wtf("VisitFormFragment", e.message)
        }


        Log.wtf("Draft ID", fieldVisitViewModel.draft_id)
        if (fieldVisitViewModel.draft_id.isNotEmpty()) {
            reportsViewModel.id = fieldVisitViewModel.draft_id
            reportsViewModel.lunchDraftApiCal()
            //fieldVisitViewModel.draft_id = draft_id
        }

        activityViewModel.allSchoolListResponse.observe(viewLifecycleOwner, Observer {

            if (it?.data?.isNotEmpty() == true) {

                if (!flag) {
                    for (i in it.data.indices) {
                        schoolIdList.add(it.data[i]?.id.toString())
                        schoolNameList.add(it.data[i]?.school_name.toString())
                        schoolZoneIdList.add(it.data[i]?.zone_id.toString())
                        schoolTypeList.add(it.data[i]?.school_type.toString())
                        schoolTotalStdList.add(it.data[i]?.total_std.toString())
                    }
                    flag = true
                }

                val spinnerArrayAdapter = ArrayAdapter(
                    requireContext(), R.layout.spinner_item, R.id.textItem, schoolNameList
                )
                binding.spinner.adapter = spinnerArrayAdapter

                if (school_id != "") {
                    for (i in schoolIdList.indices) {
                        if (school_id == schoolIdList[i]) {
                            println("School Id ${schoolIdList[i]}")
                            binding.spinner.setSelection(i)
                            binding.spinner.isEnabled = false
                            break
                        }
                    }
                }
            }
        })

        fieldVisitViewModel.addImageResponse.observe(viewLifecycleOwner, Observer {

            if (it?.response_status == "200") {
                itemCount++
                fieldVisitViewModel.imageCategoryUploadedList.add(it.data.toString())
            }

            if (itemCount != (fieldVisitViewModel.imageCategoryFile.size - fieldVisitViewModel.nullCounter)) {
                uploadImages(itemCount)
            } else {
                uploadAllInfo()
            }

        })

        fieldVisitViewModel.fieldVisitResponse.observe(viewLifecycleOwner, Observer {
            if (it?.response_status == "200") {
                if (save){
                    if (it.data != null) {
                        fieldVisitViewModel.draft_id = it.data.toString()
                        binding.stepOneBtnLay.visibility = View.GONE
                        binding.stepTwoLay.visibility = View.VISIBLE
                        binding.stepTwoBtnLay.visibility = View.VISIBLE
                    }

                    when(fieldVisitViewModel.section){

                        "2" -> {
                            binding.stepTwoBtnLay.visibility = View.GONE
                            binding.stepThreeLay.visibility = View.VISIBLE
                            binding.stepThreeBtnLay.visibility = View.VISIBLE
                        }

                        "3" -> {
                            binding.stepThreeBtnLay.visibility = View.GONE
                            binding.stepFourLay.visibility = View.VISIBLE
                            binding.stepFourBtnLay.visibility = View.VISIBLE
                        }

                        "4" -> {
                            binding.stepFourBtnLay.visibility = View.GONE
                            binding.stepFiveLay.visibility = View.VISIBLE
                        }
                    }
                    binding.spinner.isEnabled = false

                } else {
                    findNavController().navigate(R.id.action_visitFromFragment_to_auditorHomeFragment)
                }
                CustomUploadDialog.dismiss()
                Toast.makeText(requireContext(), "" + it.message, Toast.LENGTH_SHORT).show()
            } else {
                CustomUploadDialog.dismiss()
                Toast.makeText(requireContext(), "Something Went Wrong", Toast.LENGTH_SHORT).show()
            }
            Log.wtf("DRAFT ID ",fieldVisitViewModel.draft_id)
        })


        val currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date())

        binding.dateET.setText(currentDate)
        binding.timeET.setText(currentTime)

        reportsViewModel.draftResponse.observe(viewLifecycleOwner, Observer {
            var step = 0

            if (it?.message == "Success" && it.data != null) {
                step = it.data.site_visit_report.section.toInt() ?: 0

                Log.wtf("STEP",step.toString())
                binding.dateET.setText(it.data.site_visit_report.date)
                binding.timeET.setText(it.data.site_visit_report.school_time)
                fieldVisitViewModel.task_id = it.data.site_visit_report.task_id
                Log.wtf("TaskID", it.data.site_visit_report.task_id)

                //1
                if (step > 0) {

                    for (i in schoolIdList.indices) {
                        if (schoolIdList[i] == it.data.site_visit_report.school_id){
                            try {
                                binding.spinner.setSelection(i)
                                binding.spinner.isEnabled = false
                            } catch (e: Exception){/**/}
                        }
                    }

                    for (i in it.data.class_list.indices) {
                        fieldVisitViewModel.className.add(it.data.class_list[i].class_name)
                        fieldVisitViewModel.totalStudent.add(it.data.class_list[i].total)
                        fieldVisitViewModel.totalPresent.add(it.data.class_list[i].present)
                        fieldVisitViewModel.totalAbsent.add(it.data.class_list[i].absent)
                        fieldVisitViewModel.avgAge.add(it.data.class_list[i].avg_age)

                        formAddClassAdapter.getNewCount(fieldVisitViewModel.className.size)
                        binding.presentET.setText(fieldVisitViewModel.getTotalPresent().toString())
                        binding.absentET.setText(fieldVisitViewModel.getTotalAbsent().toString())
                    }

                    binding.reasonET.setText(it.data.site_visit_report.reason_absent)
                    binding.avgET.setText(it.data.site_visit_report.avg_age_std)
                    binding.totalTeacherET.setText(it.data.site_visit_report.total_teacher)
                    binding.teacherPresentET.setText(it.data.site_visit_report.present_teacher)
                    binding.teacherAbsentET.setText(it.data.site_visit_report.absent_teacher)

                    binding.stepOneBtnLay.visibility = View.GONE
                    binding.stepTwoLay.visibility = View.VISIBLE
                    binding.stepTwoBtnLay.visibility = View.VISIBLE
                }


                //2
                if (step > 1) {
                    binding.performanceET.setText(it.data.site_visit_report.perfomance)
                    binding.efficiencyET.setText(it.data.site_visit_report.efficiency_other)

                    binding.stepTwoBtnLay.visibility = View.GONE
                    binding.stepThreeLay.visibility = View.VISIBLE
                    binding.stepThreeBtnLay.visibility = View.VISIBLE
                }

                //3
                if (step > 2) {
                    for (i in it.data.food_list.indices) {
                        fieldVisitViewModel.foodlistItem.add(FoodlistItem(it.data.food_list[i].food_name))
                        foodAdapter.getNewCount(fieldVisitViewModel.foodlistItem.size)
                    }

                    for (i in it.data.nutrition_list.indices) {
                        fieldVisitViewModel.nutritionTitleList.add(it.data.nutrition_list[i].food_name)
                        fieldVisitViewModel.nutritionAmountList.add(it.data.nutrition_list[i].percent)

                        nutritionAdapter.getNewCount(fieldVisitViewModel.nutritionTitleList.size)
                    }

                    binding.approxPriceET.setText(it.data.site_visit_report.approx_meal_price)

                    binding.stepThreeBtnLay.visibility = View.GONE
                    binding.stepFourLay.visibility = View.VISIBLE
                    binding.stepFourBtnLay.visibility = View.VISIBLE
                }

                //4/5
                if (step > 3) {
                    binding.suggestImprovementET.setText(it.data.site_visit_report.suggestion)

                    for (i in it.data.compliment_list.indices) {
                        fieldVisitViewModel.foundationTitleList.add(it.data.compliment_list[i].foundation)
                        fieldVisitViewModel.foundationTeacherList.add(it.data.compliment_list[i].teacher)
                        fieldVisitViewModel.foundationImprovementList.add(it.data.compliment_list[i].issues)

                        foundationAdapter.getNewCount(fieldVisitViewModel.foundationTeacherList.size)
                    }

                    for (i in it.data.image_list.indices) {
                        fieldVisitViewModel.imageCategoryName.add(it.data.image_list[i].category)
                        fieldVisitViewModel.imageCategoryList.add((BaseUrl.URL + it.data.image_list[i].url).toUri())
                        fieldVisitViewModel.imageCategoryFile.add(null)

                        imageCategoryAdapter.getNewCount(fieldVisitViewModel.imageCategoryName.size)
                    }

                    binding.remarksET.setText(it.data.site_visit_report.remarks)

                    binding.stepFourBtnLay.visibility = View.GONE
                    binding.stepFiveLay.visibility = View.VISIBLE
                }
            }
        })


        mealQuality = "Good"
        foodItem = ""

        binding.goodRB.isChecked = true
        binding.AvgRB.isChecked = false
        binding.WorseRB.isChecked = false

        //set recycler views
        nutritionLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        nutritionAdapter = NutritionAdapter(fieldVisitViewModel)
        binding.nutritionRecyclerView.layoutManager = nutritionLayoutManager
        binding.nutritionRecyclerView.adapter = nutritionAdapter

        foodAdapter = FoodAdapter(fieldVisitViewModel)
        binding.foodRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.foodRecyclerView.adapter = foodAdapter

        foundationLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        foundationAdapter = FoundationAdapter(fieldVisitViewModel)
        binding.foundationRecyclerView.layoutManager = foundationLayoutManager
        binding.foundationRecyclerView.adapter = foundationAdapter

        addClassLayoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        formAddClassAdapter =
            FormAddClassAdapter(fieldVisitViewModel, binding.presentET, binding.absentET)
        binding.classRecyclerView.layoutManager = addClassLayoutManager
        binding.classRecyclerView.adapter = formAddClassAdapter

        imageCategoryManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        imageCategoryAdapter = ImageCategoryAdapter(fieldVisitViewModel, requireContext())
        binding.categoryImageRecycler.layoutManager = imageCategoryManager
        binding.categoryImageRecycler.adapter = imageCategoryAdapter

        //back press action
        binding.backBtn.setOnClickListener {
            requireActivity().onBackPressed()
        }

        //add nutrition action
        binding.addNutritionBtn.setOnClickListener {

            if (binding.nutritionTitle.text.toString() == "") {
                binding.nutritionTitle.requestFocus()
                binding.nutritionTitle.error = "Can't Be Empty"
                return@setOnClickListener
            }

            if (binding.nutritionAmount.text.toString() == "") {
                binding.nutritionAmount.requestFocus()
                binding.nutritionAmount.error = "Can't Be Empty"
                return@setOnClickListener
            }

            fieldVisitViewModel.nutritionTitleList.add(binding.nutritionTitle.text.toString())
            fieldVisitViewModel.nutritionAmountList.add(binding.nutritionAmount.text.toString())

            nutritionAdapter.getNewCount(fieldVisitViewModel.nutritionTitleList.size)
            binding.nutritionTitle.text.clear()
            binding.nutritionAmount.text.clear()
        }

        binding.addClassBtn.setOnClickListener {

            if (binding.classTitleET.text.toString().isEmpty()) {
                binding.classTitleET.requestFocus()
                binding.classTitleET.error = "Can't Be Empty"
                return@setOnClickListener
            }

            if (binding.totalStudentET.text.toString().isEmpty()) {
                binding.totalStudentET.requestFocus()
                binding.totalStudentET.error = "Can't Be Empty"
                return@setOnClickListener
            }

            if (binding.classPresentET.text.toString().isEmpty()) {
                binding.classPresentET.requestFocus()
                binding.classPresentET.error = "Can't Be Empty"
                return@setOnClickListener
            }

            if (binding.classAbsentET.text.toString().isEmpty()) {
                binding.classAbsentET.requestFocus()
                binding.classAbsentET.error = "Can't Be Empty"
                return@setOnClickListener
            }

            if (binding.avgAgeET.text.toString().isEmpty()) {
                binding.avgAgeET.requestFocus()
                binding.avgAgeET.error = "Can't Be Empty"
                return@setOnClickListener
            }

            fieldVisitViewModel.className.add(binding.classTitleET.text.toString())
            fieldVisitViewModel.totalStudent.add(binding.totalStudentET.text.toString())
            fieldVisitViewModel.totalPresent.add(binding.classPresentET.text.toString())
            fieldVisitViewModel.totalAbsent.add(binding.classAbsentET.text.toString())
            fieldVisitViewModel.avgAge.add(binding.avgAgeET.text.toString())

            formAddClassAdapter.getNewCount(fieldVisitViewModel.className.size)
            binding.presentET.setText(fieldVisitViewModel.getTotalPresent().toString())
            binding.absentET.setText(fieldVisitViewModel.getTotalAbsent().toString())

            binding.classTitleET.text.clear()
            binding.totalStudentET.text.clear()
            binding.classPresentET.text.clear()
            binding.classAbsentET.text.clear()
            binding.avgAgeET.text.clear()
        }

        //counter btn action
        binding.addNewTeacher.setOnClickListener {

            fieldVisitViewModel.foundationTitleList.add(binding.foundationTitleET.text.toString())
            fieldVisitViewModel.foundationTeacherList.add(binding.foundationTeacherET.text.toString())
            fieldVisitViewModel.foundationImprovementList.add(binding.foundationSuggestImprovementET.text.toString())

            foundationAdapter.getNewCount(fieldVisitViewModel.foundationTeacherList.size)
            binding.foundationTitleET.text.clear()
            binding.foundationTeacherET.text.clear()
            binding.foundationSuggestImprovementET.text.clear()
        }


        //step one Next Button action
        binding.stepOneNext.setOnClickListener {
            save = true
            stepOneButtonsAction()
        }


        //step One Draft Action
        binding.stepOneDraft.setOnClickListener {
            save = false
            stepOneButtonsAction()
        }


        //step two btn action
        binding.stepTwoNext.setOnClickListener {
            save = true
            stepTwoButtonsAction()
        }


        //step two Draft action
        binding.stepTwoDraft.setOnClickListener {
            save = false
            stepTwoButtonsAction()
        }

        binding.milkRB.setOnClickListener {
            if (!Gson().toJson(foodlistItem).contains("Milk")) {
                foodlistItem.add(FoodlistItem("Milk"))
                binding.milkRB.isChecked = true
            }
        }
        binding.breadRB.setOnClickListener {
            if (!Gson().toJson(foodlistItem).contains("Bread")) {
                foodlistItem.add(FoodlistItem("Bread"))
                binding.breadRB.isChecked = true
            }
        }
        binding.fruitRB.setOnClickListener {
            if (!Gson().toJson(foodlistItem).contains("Fruit")) {
                foodlistItem.add(FoodlistItem("Fruit"))
                binding.fruitRB.isChecked = true
            }
        }
        binding.biscuitRB.setOnClickListener {
            if (!Gson().toJson(foodlistItem).contains("Biscuit")) {
                foodlistItem.add(FoodlistItem("Biscuit"))
                binding.biscuitRB.isChecked = true
            }
        }
        binding.eggRB.setOnClickListener {
            if (!Gson().toJson(foodlistItem).contains("Egg")) {
                foodlistItem.add(FoodlistItem("Egg"))
                binding.eggRB.isChecked = true
            }
        }

        binding.goodRB.setOnClickListener {
            mealQuality = "Good"
            binding.goodRB.isChecked = true
            binding.AvgRB.isChecked = false
            binding.WorseRB.isChecked = false
        }

        binding.AvgRB.setOnClickListener {
            mealQuality = "Average"
            binding.goodRB.isChecked = false
            binding.AvgRB.isChecked = true
            binding.WorseRB.isChecked = false
        }

        binding.WorseRB.setOnClickListener {
            mealQuality = "Poor"
            binding.goodRB.isChecked = false
            binding.AvgRB.isChecked = false
            binding.WorseRB.isChecked = true
        }

        binding.addFoodBtn.setOnClickListener {
            Log.wtf("getFood", Gson().toJson(foodlistItem))
            if (foodlistItem.isEmpty()) {
                binding.milkRB.requestFocus()
                Toast.makeText(
                    requireContext(), "Select at least one food item", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            fieldVisitViewModel.foodlistItem = foodlistItem
            foodAdapter.getNewCount(fieldVisitViewModel.foodlistItem.size)

            binding.milkRB.isChecked = false
            binding.breadRB.isChecked = false
            binding.fruitRB.isChecked = false
            binding.biscuitRB.isChecked = false
            binding.eggRB.isChecked = false
        }


        //step three btn action
        binding.stepThreeNext.setOnClickListener {
            save = true
            stepThreeButtonsAction()
        }


        //step three Draft action
        binding.stepThreeDraft.setOnClickListener {
            save = false
            stepThreeButtonsAction()
        }


        //step four btn action
        binding.stepFourNext.setOnClickListener {
            save = true
            stepFourButtonsAction()
        }


        //step four btn action
        binding.stepFourDraft.setOnClickListener {
            save = false
            stepFourButtonsAction()
        }



        binding.categoryPicCard.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cacheFile = File(
                    requireActivity().externalCacheDir,
                    System.currentTimeMillis().toString() + ".jpg"
                )
                cacheFileUri = FileProvider.getUriForFile(
                    requireContext(), "com.charityright.charityauthority.provider", cacheFile!!
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cacheFileUri)
                intent.putExtra("return-data", true)
                startActivityForResult(intent, 1)
            } else {
                requestPermission()
            }
        }

        //image category radio action
        binding.foodRB.setOnClickListener {
            imageCategory = "Food & Kitchen"
            binding.foodRB.isChecked = true
            binding.classRB.isChecked = false
            binding.diningRB.isChecked = false
            binding.attendanceRB.isChecked = false
            binding.groupRB.isChecked = false
        }

        binding.classRB.setOnClickListener {
            imageCategory = "Class Education"
            binding.foodRB.isChecked = false
            binding.classRB.isChecked = true
            binding.diningRB.isChecked = false
            binding.attendanceRB.isChecked = false
            binding.groupRB.isChecked = false
        }

        binding.diningRB.setOnClickListener {
            imageCategory = "Dining"
            binding.foodRB.isChecked = false
            binding.classRB.isChecked = false
            binding.diningRB.isChecked = true
            binding.attendanceRB.isChecked = false
            binding.groupRB.isChecked = false
        }

        binding.attendanceRB.setOnClickListener {
            imageCategory = "Attendance Sheet"
            binding.foodRB.isChecked = false
            binding.classRB.isChecked = false
            binding.diningRB.isChecked = false
            binding.attendanceRB.isChecked = true
            binding.groupRB.isChecked = false
        }

        binding.groupRB.setOnClickListener {
            imageCategory = "Group/Play Time"
            binding.foodRB.isChecked = false
            binding.classRB.isChecked = false
            binding.diningRB.isChecked = false
            binding.attendanceRB.isChecked = false
            binding.groupRB.isChecked = true
        }

        binding.addNewPicture.setOnClickListener {

            if (imageCategory != "" && fieldVisitViewModel.imageCategoryName.size != 5 && cacheFile != null && cacheFileUri != null) {
                fieldVisitViewModel.imageCategoryName.add(imageCategory)
                fieldVisitViewModel.imageCategoryFile.add(cacheFile)
                fieldVisitViewModel.imageCategoryList.add(cacheFileUri!!)

                imageCategoryAdapter.getNewCount(fieldVisitViewModel.imageCategoryName.size)
                clearImageInfo()

            } else {
                Toast.makeText(
                    requireContext(),
                    "Please Select At Least One Image And Maximum 5 Image",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

        //submit btn action
        binding.submitBtn.setOnClickListener {
            fieldVisitViewModel.status = "1"
            save = false
            stepFiveButtonsAction(1)
        }


        //stepFive Draft action
        binding.stepFiveDraft.setOnClickListener {
            fieldVisitViewModel.status = "0"
            save = false
            stepFiveButtonsAction(0)
        }


        return binding.root
    }

    private fun postDataToApi() {
        Log.wtf("SECTION", fieldVisitViewModel.section)
        //fieldVisitViewModel.draft_id = draft_id
        fieldVisitViewModel.as_time = as_time
        fieldVisitViewModel.zone_id = schoolZoneIdList[binding.spinner.selectedItemPosition]
        fieldVisitViewModel.school_id = schoolIdList[binding.spinner.selectedItemPosition]
        fieldVisitViewModel.school_time = binding.timeET.text.toString()
        fieldVisitViewModel.date = binding.dateET.text.toString()
        fieldVisitViewModel.total_std = schoolTotalStdList[binding.spinner.selectedItemPosition]
        fieldVisitViewModel.present_std = binding.presentET.text.toString()
        fieldVisitViewModel.absent_std = binding.absentET.text.toString()
        fieldVisitViewModel.reason_absent = binding.reasonET.text.toString()
        fieldVisitViewModel.avg_age_std = binding.avgET.text.toString()
        fieldVisitViewModel.edu_type = schoolTypeList[binding.spinner.selectedItemPosition]
        fieldVisitViewModel.performance = binding.performanceET.text.toString()
        fieldVisitViewModel.efficiency_other = binding.efficiencyET.text.toString()
        fieldVisitViewModel.approx_meal_price = binding.approxPriceET.text.toString()
        fieldVisitViewModel.meal_quality = mealQuality
        fieldVisitViewModel.foodlist = Gson().toJson(foodlistItem)
        fieldVisitViewModel.totalTeacher = binding.totalTeacherET.text.toString()
        fieldVisitViewModel.presentTeacher = binding.teacherPresentET.text.toString()
        fieldVisitViewModel.absentTeacher = binding.teacherAbsentET.text.toString()

        fieldVisitViewModel.suggestion = binding.suggestImprovementET.text.toString()
        fieldVisitViewModel.nutrition_list = getNutritionJsonObject().toString()
        fieldVisitViewModel.complaints_list = getComplaintsJsonObject().toString()
        fieldVisitViewModel.class_list = getClassJsonObject().toString()
        fieldVisitViewModel.remarks = binding.remarksET.text.toString()

        CustomUploadDialog.show()

        when (fieldVisitViewModel.section) {
            "5" -> uploadImages(itemCount)
            else -> uploadAllInfo()
        }
    }


    // Step Ones Buttons Action (0 for draft, 1 for next)
    private fun stepOneButtonsAction() {
        if (binding.presentET.text.toString() == "") {
            binding.presentET.requestFocus()
            binding.presentET.error = "Can't Be Empty"
            return
        }

        if (binding.absentET.text.toString() == "") {
            binding.absentET.requestFocus()
            binding.absentET.error = "Can't Be Empty"
            return
        }

        if (binding.reasonET.text.toString() == "") {
            binding.reasonET.requestFocus()
            binding.reasonET.error = "Can't Be Empty"
            return
        }

        if (binding.avgET.text.toString() == "") {
            binding.avgET.requestFocus()
            binding.avgET.error = "Can't Be Empty"
            return
        }

        if (binding.totalTeacherET.text.toString() == "") {
            binding.totalTeacherET.requestFocus()
            binding.totalTeacherET.error = "Can't Be Empty"
            return
        }
        if (binding.teacherPresentET.text.toString() == "") {
            binding.teacherPresentET.requestFocus()
            binding.teacherPresentET.error = "Can't Be Empty"
            return
        }
        if (binding.teacherAbsentET.text.toString() == "") {
            binding.teacherAbsentET.requestFocus()
            binding.teacherAbsentET.error = "Can't Be Empty"
            return
        }

        /*binding.stepOneBtnLay.visibility = View.GONE
        binding.stepTwoLay.visibility = View.VISIBLE
        binding.stepTwoBtnLay.visibility = View.VISIBLE*/

        fieldVisitViewModel.section = "1"
        fieldVisitViewModel.status = "0"
        postDataToApi()
    }


    //Step Two Button Action
    private fun stepTwoButtonsAction() {
        if (binding.presentET.text.toString() == "") {
            binding.presentET.requestFocus()
            binding.presentET.error = "Can't Be Empty"
            return
        }

        if (binding.absentET.text.toString() == "") {
            binding.absentET.requestFocus()
            binding.absentET.error = "Can't Be Empty"
            return
        }

        if (binding.reasonET.text.toString() == "") {
            binding.reasonET.requestFocus()
            binding.reasonET.error = "Can't Be Empty"
            return
        }

        if (binding.avgET.text.toString() == "") {
            binding.avgET.requestFocus()
            binding.avgET.error = "Can't Be Empty"
            return
        }

        if (binding.performanceET.text.toString() == "") {
            binding.performanceET.requestFocus()
            binding.performanceET.error = "Can't Be Empty"
            return
        }

        if (binding.efficiencyET.text.toString() == "") {
            binding.efficiencyET.requestFocus()
            binding.efficiencyET.error = "Can't Be Empty"
            return
        }

        /*binding.stepTwoBtnLay.visibility = View.GONE
        binding.stepThreeLay.visibility = View.VISIBLE
        binding.stepThreeBtnLay.visibility = View.VISIBLE*/

        fieldVisitViewModel.section = "2"
        fieldVisitViewModel.status = "0"
        postDataToApi()
    }


    //Step Three Button Action
    private fun stepThreeButtonsAction() {
        if (binding.presentET.text.toString() == "") {
            binding.presentET.requestFocus()
            binding.presentET.error = "Can't Be Empty"
            return
        }

        if (binding.absentET.text.toString() == "") {
            binding.absentET.requestFocus()
            binding.absentET.error = "Can't Be Empty"
            return
        }

        if (binding.reasonET.text.toString() == "") {
            binding.reasonET.requestFocus()
            binding.reasonET.error = "Can't Be Empty"
            return
        }

        if (binding.avgET.text.toString() == "") {
            binding.avgET.requestFocus()
            binding.avgET.error = "Can't Be Empty"
            return
        }

        if (binding.performanceET.text.toString() == "") {
            binding.performanceET.requestFocus()
            binding.performanceET.error = "Can't Be Empty"
            return
        }

        if (binding.efficiencyET.text.toString() == "") {
            binding.efficiencyET.requestFocus()
            binding.efficiencyET.error = "Can't Be Empty"
            return
        }

        if (binding.approxPriceET.text.toString() == "") {
            binding.approxPriceET.requestFocus()
            binding.approxPriceET.error = "Cant Be Empty"
            return
        }

        /*binding.stepThreeBtnLay.visibility = View.GONE
        binding.stepFourLay.visibility = View.VISIBLE
        binding.stepFourBtnLay.visibility = View.VISIBLE*/

        fieldVisitViewModel.section = "3"
        fieldVisitViewModel.status = "0"
        postDataToApi()
    }


    // Step Four Buttons Action (0 for draft, 1 for next)
    private fun stepFourButtonsAction() {
        if (binding.presentET.text.toString() == "") {
            binding.presentET.requestFocus()
            binding.presentET.error = "Can't Be Empty"
            return
        }

        if (binding.absentET.text.toString() == "") {
            binding.absentET.requestFocus()
            binding.absentET.error = "Can't Be Empty"
            return
        }

        if (binding.reasonET.text.toString() == "") {
            binding.reasonET.requestFocus()
            binding.reasonET.error = "Can't Be Empty"
            return
        }

        if (binding.avgET.text.toString() == "") {
            binding.avgET.requestFocus()
            binding.avgET.error = "Can't Be Empty"
            return
        }

        if (binding.performanceET.text.toString() == "") {
            binding.performanceET.requestFocus()
            binding.performanceET.error = "Can't Be Empty"
            return
        }

        if (binding.efficiencyET.text.toString() == "") {
            binding.efficiencyET.requestFocus()
            binding.efficiencyET.error = "Can't Be Empty"
            return
        }

        if (binding.approxPriceET.text.toString() == "") {
            binding.approxPriceET.requestFocus()
            binding.approxPriceET.error = "Can't Be Empty"
            return
        }

        /*binding.stepFourBtnLay.visibility = View.GONE
        binding.stepFiveLay.visibility = View.VISIBLE*/

        fieldVisitViewModel.section = "4"
        fieldVisitViewModel.status = "0"
        postDataToApi()
    }


    // Step Ones Buttons Action (0 for draft, 1 for next)
    private fun stepFiveButtonsAction(action: Int) {
        if (binding.presentET.text.toString() == "") {
            binding.presentET.requestFocus()
            binding.presentET.error = "Can't Be Empty"
            return
        }

        if (binding.absentET.text.toString() == "") {
            binding.absentET.requestFocus()
            binding.absentET.error = "Can't Be Empty"
            return
        }

        if (binding.reasonET.text.toString() == "") {
            binding.reasonET.requestFocus()
            binding.reasonET.error = "Can't Be Empty"
            return
        }

        if (binding.avgET.text.toString() == "") {
            binding.avgET.requestFocus()
            binding.avgET.error = "Can't Be Empty"
            return
        }

        if (binding.performanceET.text.toString() == "") {
            binding.performanceET.requestFocus()
            binding.performanceET.error = "Can't Be Empty"
            return
        }

        if (binding.efficiencyET.text.toString() == "") {
            binding.efficiencyET.requestFocus()
            binding.efficiencyET.error = "Can't Be Empty"
            return
        }

        if (binding.approxPriceET.text.toString() == "") {
            binding.approxPriceET.requestFocus()
            binding.approxPriceET.error = "Can't Be Empty"
            return
        }

        if (fieldVisitViewModel.imageCategoryName.isEmpty()) {
            Toast.makeText(
                requireContext(), "Please Add Data In Image Category List", Toast.LENGTH_SHORT
            ).show()
            return
        }

        /*if (binding.remarksET.text.toString() == ""){
            binding.remarksET.requestFocus()
            binding.remarksET.error = "Can't Be Empty"
            return@setOnClickListener
        }*/

        if (itemCount > 10) {
            Toast.makeText(
                requireContext(), "10 Maximum Image Can be Chosen", Toast.LENGTH_SHORT
            ).show()
            return
        }

        Log.wtf("foodlistCheck", Gson().toJson(foodlistItem))
        Log.wtf("complainListCheck", getComplaintsJsonObject().toString())
        Log.wtf(
            "teacherCheck",
            binding.totalTeacherET.text.toString() + " , " + binding.teacherPresentET.text.toString() + " , " + binding.teacherAbsentET.text.toString()
        )


        fieldVisitViewModel.section = "5"
        postDataToApi()
    }


    private fun uploadAllInfo() {

        for (i in fieldVisitViewModel.imageCategoryFile.indices) {
            fieldVisitViewModel.imageCategoryFile[i]?.delete()
        }

        fieldVisitViewModel.images = getImageJsonObject().toString()

        if (fieldVisitViewModel.section == "1") {
            lifecycleScope.launch {
                fieldVisitViewModel.launchAddFieldApiCall()
                Log.wtf("ADD","")
            }
        } else {

            lifecycleScope.launch {
                fieldVisitViewModel.launchUpdateFieldApiCall()
                Log.wtf("Update","")
            }
        }
    }

    private fun uploadImages(index: Int) {

        fieldVisitViewModel.nullCounter = 0

        for (i in fieldVisitViewModel.imageCategoryFile.indices) {
            if (fieldVisitViewModel.imageCategoryFile[i] == null) {
                fieldVisitViewModel.nullCounter++
            }
        }

        if (index < fieldVisitViewModel.imageCategoryFile.size) {

            if (fieldVisitViewModel.imageCategoryFile.size == fieldVisitViewModel.nullCounter) {

                uploadAllInfo()

            } else {

                if (fieldVisitViewModel.imageCategoryFile[index] == null) {

                    uploadImages(index + 1)
                }

                if (fieldVisitViewModel.imageCategoryFile[index] != null) {

                    lifecycleScope.launch {
                        val compressedImageFile = Compressor.compress(
                            requireContext(), fieldVisitViewModel.imageCategoryFile[index]!!
                        ) {
                            resolution(512, 512)
                            quality(50)
                        }
                        fieldVisitViewModel.launchImageUploadApiCall(compressedImageFile)
                    }
                }
            }
        } else {
            postDataToApi()
        }
    }

    private fun clearImageInfo() {

        imageCategory = ""
        binding.foodRB.isChecked = false
        binding.classRB.isChecked = false
        binding.diningRB.isChecked = false
        binding.attendanceRB.isChecked = false
        binding.groupRB.isChecked = false

        binding.imageView1.setImageDrawable(
            ContextCompat.getDrawable(
                requireContext(), R.drawable.add_image
            )
        )

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                1 -> {
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(
                            requireContext().contentResolver, cacheFileUri
                        )
                        val temp = getResizedBitmap(bitmap, 120, 120)
                        binding.imageView1.setImageBitmap(temp)

                    } catch (e: Exception) {
                        Log.wtf("FiledVisitFormFragment", "onActivityResult: ", e)
                    }
                }
            }
        }
    }

    private fun requestPermission() {
        when {
            shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    private fun getResizedBitmap(bm: Bitmap, newHeight: Int, newWidth: Int): Bitmap? {
        val width = bm.width
        val height = bm.height
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight)
        // RECREATE THE NEW BITMAP
        return Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false
        )
    }

    private fun getNutritionJsonObject(): JsonArray? {
        val gson = GsonBuilder().create()

        nutritionArrayList.clear()

        for (i in fieldVisitViewModel.nutritionTitleList.indices) {
            val tempAddNutritionName = NutritionModel(
                fieldVisitViewModel.nutritionTitleList[i],
                fieldVisitViewModel.nutritionAmountList[i]
            )
            nutritionArrayList.add(tempAddNutritionName)
        }

        return gson.toJsonTree(nutritionArrayList).asJsonArray
    }

    private fun getComplaintsJsonObject(): JsonArray? {
        val gson = GsonBuilder().create()

        complaintsArrayList.clear()

        for (i in fieldVisitViewModel.foundationTitleList.indices) {
            val tempAddComplaintsName = ComplaintsModel(
                fieldVisitViewModel.foundationTitleList[i],
                fieldVisitViewModel.foundationTeacherList[i],
                fieldVisitViewModel.foundationImprovementList[i]
            )
            complaintsArrayList.add(tempAddComplaintsName)
        }

        return gson.toJsonTree(complaintsArrayList).asJsonArray
    }

    private fun getImageJsonObject(): JsonArray? {
        val gson = GsonBuilder().create()

        imageArrayList.clear()

        for (i in fieldVisitViewModel.imageCategoryUploadedList.indices) {
            val tempImageList = FieldImageModel(
                fieldVisitViewModel.imageCategoryName[i],
                fieldVisitViewModel.imageCategoryUploadedList[i]
            )
            imageArrayList.add(tempImageList)
        }

        return gson.toJsonTree(imageArrayList).asJsonArray
    }

    private fun getClassJsonObject(): JsonArray? {
        val gson = GsonBuilder().create()

        classArrayList.clear()

        for (i in fieldVisitViewModel.className.indices) {
            val tempClassList = FormClassModel(
                fieldVisitViewModel.className[i],
                fieldVisitViewModel.totalStudent[i],
                fieldVisitViewModel.totalPresent[i],
                fieldVisitViewModel.totalAbsent[i],
                fieldVisitViewModel.avgAge[i]
            )
            classArrayList.add(tempClassList)
        }

        return gson.toJsonTree(classArrayList).asJsonArray

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission has been granted. Start camera preview Activity.
            Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            // Permission request was denied.
            Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }
}