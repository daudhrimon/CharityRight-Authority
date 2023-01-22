package com.charityright.charityauthority.auditor.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.charityright.charityauthority.auditor.model.DraftResponse.DraftResponse
import com.charityright.charityauthority.model.admin.allAuditReport.AllAuditReportBaseResponse
import com.charityright.charityauthority.retrofit.AuditorRequestInterface
import com.charityright.charityauthority.util.CustomDialog
import com.charityright.charityauthority.util.CustomSharedPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.await

class ReportsViewModel(application: Application): AndroidViewModel(application) {
    val reportsResponse: LiveData<AllAuditReportBaseResponse?> = MutableLiveData()
    val draftResponse: LiveData<DraftResponse?> = MutableLiveData()

    var status = ""
    var id = ""

    fun launchApiCall(){
        reportsResponse as MutableLiveData
        CustomDialog.show()

        viewModelScope.launch(Dispatchers.IO){
            try {
                reportsResponse.postValue(getReportResponse())

            }catch (e: Exception){
                Log.wtf("ReportsViewModelSubmitted",e.message)
            }

            withContext(Dispatchers.Main){
                CustomDialog.dismiss()
            }
        }

    }


    fun lunchDraftApiCal(){
        draftResponse as MutableLiveData
        CustomDialog.show()

        viewModelScope.launch(Dispatchers.IO){
            try {
                draftResponse.postValue(getDraftResponse())

            } catch (e: Exception){
                Log.wtf("ReportsViewModelDraft",e.message)
            }

            withContext(Dispatchers.Main){
                CustomDialog.dismiss()
            }
        }
    }

    private suspend fun getReportResponse(): AllAuditReportBaseResponse?{
        val response = AuditorRequestInterface().getReportList("Bearer "+ CustomSharedPref.read("TOKEN",""),status)
        return response.await()
    }

    private suspend fun getDraftResponse(): DraftResponse?{
        val response = AuditorRequestInterface().getDraftResponse("Bearer "+ CustomSharedPref.read("TOKEN",""),id)
        return response.await()
    }
}