package com.charityright.charityauthority.auditor.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.charityright.charityauthority.adapters.AuditorReportAdapter
import com.charityright.charityauthority.auditor.viewModel.ReportsViewModel
import com.charityright.charityauthority.databinding.FragmentSubmittedReportBinding
import com.charityright.charityauthority.util.CustomDialog

class SubmittedReportFragment : Fragment() {
    private var _binding: FragmentSubmittedReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var auditorReportAdapter: AuditorReportAdapter

    private lateinit var reportsViewModel: ReportsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding =  FragmentSubmittedReportBinding.inflate(inflater, container, false)

        reportsViewModel = ViewModelProvider(this).get(ReportsViewModel::class.java)
        CustomDialog.init(requireContext())


        reportsViewModel.reportsResponse.observe(viewLifecycleOwner, Observer {
            if (it?.data?.isNotEmpty() == true){

                binding.recyclerView.visibility = View.VISIBLE
                binding.noDataLayout.visibility = View.GONE

                layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
                auditorReportAdapter = AuditorReportAdapter(
                    findNavController(),
                    it.data,
                    "submit"
                )
                binding.recyclerView.layoutManager = layoutManager
                binding.recyclerView.adapter = auditorReportAdapter
            }else{
                binding.recyclerView.visibility = View.GONE
                binding.noDataLayout.visibility = View.VISIBLE
            }
        })


        binding.backBtn.setOnClickListener { findNavController().popBackStack() }

        //set status 1 to get submitted Reports
        reportsViewModel.status = "1"
        reportsViewModel.launchApiCall()


        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}