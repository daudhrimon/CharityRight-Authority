package com.charityright.charityauthority.adapters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.charityright.charityauthority.R
import com.charityright.charityauthority.databinding.AuditorReportItemBinding
import com.charityright.charityauthority.model.admin.allAuditReport.Data
import com.charityright.charityauthority.retrofit.BaseUrl
import com.squareup.picasso.Picasso

class AuditorReportAdapter(
    private val findNavController: NavController,
    private val data: List<Data?>,
    private val flag: String
) : RecyclerView.Adapter<AuditorReportAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AuditorReportItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        Picasso.get().load("${BaseUrl.URL}${data[position]?.site_image}").placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder).into(holder.binding.imageView)
        holder.binding.titleTV.text = data[position]?.report_title
        holder.binding.AuditorTV.text = data[position]?.auditor_name
        holder.binding.dateTV.text = "Date: ${data[position]?.date}"
        holder.binding.zoneTV.text = "Zone: ${data[position]?.audit_zone}"


        holder.itemView.setOnClickListener {
            val bundle = Bundle()

            when(flag){

                "list" -> {
                    bundle.putString("url",data[position]?.url)
                    bundle.putString("preview_url",data[position]?.preview_url)
                    bundle.putString("title",data[position]?.report_title)
                    findNavController.navigate(R.id.action_auditorReportListFragment_to_auditorReportDownloadFragment,bundle)
                }

                "report" -> {
                    bundle.putString("url",data[position]?.url)
                    bundle.putString("preview_url",data[position]?.preview_url)
                    bundle.putString("title",data[position]?.report_title)
                    findNavController.navigate(R.id.action_auditorReportFragment_to_auditorReportDownloadFragment,bundle)
                }

                "submit" -> {
                    bundle.putString("url",data[position]?.url)
                    bundle.putString("preview_url",data[position]?.preview_url)
                    bundle.putString("title",data[position]?.report_title)
                    findNavController.navigate(R.id.submittedReportsFrag2auditorReportDownloadFrag,bundle)
                }

                "draft" -> {
                    bundle.putString("draft_id",data[position]?.id)
                    findNavController.navigate(R.id.draftReportFrag2visitFromFrag,bundle)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(val binding: AuditorReportItemBinding) : RecyclerView.ViewHolder(binding.root)
}