package com.minar.birday.fragments

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textview.MaterialTextView
import com.minar.birday.R


class DNDFragment : BottomSheetDialogFragment() {

    private lateinit var notificationManager: NotificationManager
    lateinit var dndSwitch: Switch

    private var isDndOn = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_d_n_d, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {

            dndSwitch = view.findViewById(R.id.switch_dnd)
            val add_app_dnd: Button = view.findViewById(R.id.add_app_dnd)

            notificationManager =
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            add_app_dnd.setOnClickListener {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            }

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
                        isDndOn = true
                        dndSwitch.isChecked = true
                        updateDndStatus()
                    }
                }else{
                    disableDND()
                }
            } else {
                isDndOn=false
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
                }
            }

            // Set up call listener
            setUpCallListener()
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
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ANSWER_PHONE_CALLS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), 2)
            }else {
                dndSwitch.isChecked = true
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                updateDndStatus()
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
        }
    }

    private fun updateDndStatus() {
        if (isDndOn) {
            checkRunningAppsAndRejectCalls()
        }
    }

    private fun checkRunningAppsAndRejectCalls() {
        val activityManager = requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = activityManager.runningAppProcesses

        val dndApps = listOf("com.zoom.us", "com.microsoft.teams")

        for (app in runningApps) {
            if (dndApps.contains(app.processName)) {
                // Logic to handle call rejection
                setUpCallListener()
                break
            }
        }
    }

    private fun setUpCallListener() {
        val telephonyManager = requireActivity().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                if (state == TelephonyManager.CALL_STATE_RINGING && isDndOn) {
                    // Reject the call
                    rejectCall()
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun rejectCall() {
        // This may require special permissions and API-level support
        val telephonyManager = requireActivity().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val telecomManager = requireActivity().getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        val phoneStateListener = object : PhoneStateListener() {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                if (state == TelephonyManager.CALL_STATE_RINGING) {
                    if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                        telecomManager.endCall() // End the current call
                        println("Call rejected")
                    } else {
                        // Handle the case where the permission is not granted
                        println("Permission not granted for rejecting calls")
                    }
                }
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 2) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                isDndOn = true
                dndSwitch.isChecked = true
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                updateDndStatus()
            } else {
                // Permission denied, handle the case accordingly
                Toast.makeText(requireContext(), "Kindly Grant Permission!.", Toast.LENGTH_SHORT).show()
            }
        }
    }


}