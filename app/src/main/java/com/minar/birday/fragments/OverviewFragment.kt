package com.minar.birday.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.databinding.FragmentOverviewBinding
import com.minar.birday.viewmodels.MainViewModel
import java.time.LocalDate


@ExperimentalStdlibApi
class OverviewFragment : Fragment() {
    private lateinit var act: MainActivity
    private val mainViewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        act = activity as MainActivity
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Reset the binding to null to follow the best practice
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentOverviewBinding.inflate(inflater, container, false)

        val title: String = getString(R.string.overview) + " - ${LocalDate.now().year}"
        binding.overviewTitle.text = title

        val august = binding.overviewAug
        august.highlightDay(
            15,
            act.getThemeColor(R.attr.colorPrimary)
        )
        return binding.root
    }
}