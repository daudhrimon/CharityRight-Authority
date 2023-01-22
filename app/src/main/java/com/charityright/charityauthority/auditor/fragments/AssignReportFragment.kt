package com.charityright.charityauthority.auditor.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.charityright.charityauditor.Adapters.AssignReportAdapter
import com.charityright.charityauthority.auditor.viewModel.AuditorActivityViewModel
import com.charityright.charityauthority.databinding.FragmentAssignReportBinding
import com.charityright.charityauthority.util.CustomDialog
import kotlinx.coroutines.launch


class AssignReportFragment : Fragment() {

    private var _binding: FragmentAssignReportBinding? = null
    private val binding get() = _binding!!

    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var assignReportAdapter: AssignReportAdapter

    private lateinit var auditorActivityViewModel: AuditorActivityViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssignReportBinding.inflate(inflater,container,false)
        CustomDialog.init(requireContext())

        binding.header.text = arguments?.getString("tag") ?: "Upcoming Assignment"

        auditorActivityViewModel = ViewModelProvider(requireActivity()).get(AuditorActivityViewModel::class.java)


        // passing 0 to get Assign Report and 1 to get Upcoming Assignment
        auditorActivityViewModel.action = arguments?.getInt("action") ?: 1

        auditorActivityViewModel.assignedReportResponse.observe(viewLifecycleOwner, Observer{
            layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.VERTICAL,false)
            assignReportAdapter = AssignReportAdapter(findNavController(),it?.data ?: emptyList())
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.adapter = assignReportAdapter
        })


        binding.backBtn.setOnClickListener { findNavController().popBackStack() }


        auditorActivityViewModel.launchApiCall()


        return binding.root
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}