package com.charityright.charityauthority.auditor.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.charityright.charityauthority.AuthenticationActivity
import com.charityright.charityauthority.R
import com.charityright.charityauthority.auditor.viewModel.AuditorActivityViewModel
import com.charityright.charityauthority.databinding.FragmentDonorHomeBinding
import com.charityright.charityauthority.util.CustomSharedPref


class AuditorHomeFragment : Fragment() {

    private var _binding: FragmentDonorHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var auditorActivityViewModel: AuditorActivityViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonorHomeBinding.inflate(inflater,container,false)

        auditorActivityViewModel = ViewModelProvider(requireActivity()).get(AuditorActivityViewModel::class.java)

        binding.menuBtn.setOnClickListener {
            if (binding.homeDrawer.isDrawerOpen(GravityCompat.END)){
                binding.homeDrawer.closeDrawer(GravityCompat.START)
            }else{
                binding.homeDrawer.openDrawer(GravityCompat.START)
            }
        }

        binding.remarkBtn.setOnClickListener {
            findNavController().navigate(R.id.action_donorHomeFragment2_to_beneficiaryFragment)
        }

        binding.assignBtn.setOnClickListener {
            if (auditorActivityViewModel.lat == ""){
                Toast.makeText(requireContext(),"Location Is Not Fetched Yet",Toast.LENGTH_SHORT).show()
                requireActivity().recreate()
            }else{
                val bundle = Bundle()
                bundle.putString("tag","Assign Report")
                bundle.putInt("action",0)
                findNavController().navigate(R.id.action_donorHomeFragment2_to_assignReportFragment,bundle)
            }

        }

        binding.SpecialBtn.setOnClickListener {
            findNavController().navigate(R.id.action_donorHomeFragment2_to_specialCircumstanceFragment)
        }

        binding.FieldVisitBtn.setOnClickListener {
            findNavController().navigate(R.id.action_donorHomeFragment2_to_visitFromFragment)
        }

        binding.logoutBtn.setOnClickListener {
            CustomSharedPref.write("TOKEN",null)
            CustomSharedPref.write("TYPE",null)
            startActivity(Intent(requireContext(), AuthenticationActivity::class.java))
            requireActivity().finish()
        }

        binding.notificationBtn.setOnClickListener {
            findNavController().navigate(R.id.action_auditorHomeFragment_to_notificationFragment2)
        }


        binding.navDrawer.setupWithNavController(findNavController())

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}