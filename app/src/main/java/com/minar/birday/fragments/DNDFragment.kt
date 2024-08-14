package com.minar.birday.fragments

import android.Manifest
import android.app.AppOpsManager
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.receivers.AppMonitoringService


class DNDFragment : BottomSheetDialogFragment() {

    private lateinit var notificationManager: NotificationManager
    lateinit var dndSwitch: Switch

    lateinit var act: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_d_n_d, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Make sure the fragment is properly dismissed to avoid memory leaks
        dialog?.dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {

            dndSwitch = view.findViewById(R.id.switch_dnd)
            val add_app_dnd: Button = view.findViewById(R.id.add_app_dnd)
            val homeMiniFab = view.findViewById<FloatingActionButton>(R.id.homeMiniFab)
            // Set motion layout state, since it's saved

            notificationManager =
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            add_app_dnd.setOnClickListener {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }

            act = activity as MainActivity

            // Vibration on the mini fab (with manual managing of the transition)
            homeMiniFab.setOnClickListener {
                act.vibrate()
                // Replace the fragment
                dismiss()
                val intent = Intent(requireActivity(), MainActivity::class.java)
                startActivity(intent)
            }

            //it was automatically reject the call for all apps
            if (isDndEnabled(requireContext())) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ANSWER_PHONE_CALLS
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissions(arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), 2)
                    } else {
                        dndSwitch.isChecked = true
                        startMonitoringService()
                    }
                } else {
                    disableDND()
                    stopMonitoringService()
                }
            } else {
                dndSwitch.isChecked = false
            }


            dndSwitch.setOnCheckedChangeListener { _, isChecked ->
                // Check the current DND state and update the switch
                dndSwitch.isChecked =
                    notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE

                if (isChecked) {
                    enableDND()
                } else {
                    disableDND()
                    stopMonitoringService()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun isDndEnabled(context: Context): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        return when (notificationManager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> true

            else -> false
        }
    }


    private fun enableDND() {
        // Check if the app has permission to modify DND settings
        if (notificationManager.isNotificationPolicyAccessGranted) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ANSWER_PHONE_CALLS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), 2)
            } else {
                dndSwitch.isChecked = true
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                startMonitoringService()
            }
        } else {
            Toast.makeText(
                requireContext(),
                "Kindly Grant the DND permission for this app!",
                Toast.LENGTH_SHORT
            ).show()
            // If not, request permission from the user
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivityForResult(intent, 1)
        }
    }

    private fun disableDND() {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            dndSwitch.isChecked = false
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            enableDND()
        } else if (requestCode == 4) {
            dndSwitch.isChecked = true
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            startMonitoringService()
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(requireContext(), AppMonitoringService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopMonitoringService() {
        val intent = Intent(requireContext(), AppMonitoringService::class.java)
        requireContext().stopService(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.READ_PHONE_STATE
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 3)
                }
            } else {
                // Permission denied, handle the case accordingly
                Toast.makeText(requireContext(), "Kindly Grant Permission!.", Toast.LENGTH_SHORT)
                    .show()
            }
        } else if (requestCode == 3) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                if (!hasUsageStatsPermission()) {
                    requestUsageStatsPermission()
                } else {
                    dndSwitch.isChecked = true
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    startMonitoringService()
                }

            } else {
                // Permission denied, handle the case accordingly
                Toast.makeText(requireContext(), "Kindly Grant Permission!.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    //to detect meeting apps like zoom or teams
    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager =
            requireActivity().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        Toast.makeText(requireContext(),"Kindly Grant permission for this app!",Toast.LENGTH_SHORT).show()
        startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), 4)
    }

}