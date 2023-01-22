package com.charityright.charityauthority.auditor.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.charityright.charityauthority.auditor.model.AssignedReport.AssignedReportBaseModel
import com.charityright.charityauthority.auditor.model.SchoolListBaseResponse.SchoolListBaseResponse
import com.charityright.charityauthority.retrofit.AuditorRequestInterface
import com.charityright.charityauthority.util.CustomDialog
import com.charityright.charityauthority.util.CustomSharedPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.await
import java.lang.Exception

class AuditorActivityViewModel(application: Application) : AndroidViewModel(application) {

    val assignedReportResponse: LiveData<AssignedReportBaseModel?> = MutableLiveData()
    val allSchoolListResponse: LiveData<SchoolListBaseResponse?> = MutableLiveData()

    var action = 1
    var lat = ""
    var lon = ""

    // type O for assignReport and
    fun launchApiCall(){
        assignedReportResponse as MutableLiveData
        allSchoolListResponse as MutableLiveData

        CustomDialog.show()

        viewModelScope.launch(Dispatchers.IO){
            try {
                when(action){
                    0->  assignedReportResponse.postValue(getAssignedReportResponse())
                    1-> assignedReportResponse.postValue(getUpcomingAssignResponse())
                }
                allSchoolListResponse.postValue(getAllSchoolListResponse())

            } catch (e: Exception){
                Log.wtf("AuditorActivityViewModel",e.message)
            }

            withContext(Dispatchers.Main){
                CustomDialog.dismiss()
            }
        }
    }

    private suspend fun getAllSchoolListResponse(): SchoolListBaseResponse {
        val response = AuditorRequestInterface().schoolList("Bearer "+ CustomSharedPref.read("TOKEN",""))
        return response.await()
    }

    private suspend fun getAssignedReportResponse(): AssignedReportBaseModel {
        val response = AuditorRequestInterface().assignedReportList("Bearer "+ CustomSharedPref.read("TOKEN",""))
        return response.await()
    }

    private suspend fun getUpcomingAssignResponse(): AssignedReportBaseModel {
        val response = AuditorRequestInterface().upcomingAssignList("Bearer "+ CustomSharedPref.read("TOKEN",""))
        return response.await()
    }
}