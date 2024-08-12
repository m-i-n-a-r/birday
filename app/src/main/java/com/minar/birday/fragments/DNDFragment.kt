package com.minar.birday.fragments

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textview.MaterialTextView
import com.minar.birday.R
import com.minar.birday.activities.MainActivity
import com.minar.birday.fragments.dialogs.QuickAppsBottomSheet
import com.minar.birday.utilities.resultToEvent

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DNDFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DNDFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var notificationManager: NotificationManager
    lateinit var dndSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_d_n_d, container, false)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DNDFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DNDFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dndSwitch = view.findViewById(R.id.switch_dnd)
        val add_app_dnd: Button = view.findViewById(R.id.add_app_dnd)
        val dnd_apps_rv: RecyclerView = view.findViewById(R.id.dnd_apps_Recycler)
        val no_data: MaterialTextView = view.findViewById(R.id.no_apps)

        notificationManager = requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        add_app_dnd.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }

        if(isDndEnabled(requireContext())){
            dndSwitch.isChecked=true
            val telephonyManager = requireActivity().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    super.onCallStateChanged(state, phoneNumber)
                    val isDNDActive = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                    if (isDNDActive && state == TelephonyManager.CALL_STATE_RINGING) {
                        // End the call
                        endCall()
                    }
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }else{
            dndSwitch.isChecked=false
        }

        dndSwitch.setOnCheckedChangeListener { _, isChecked ->
            // Check the current DND state and update the switch
            dndSwitch.isChecked = notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE

            if (isChecked) {
                enableDND()
            } else {
                disableDND()
            }
        }

        dnd_apps_rv.visibility=View.GONE
        no_data.visibility=View.VISIBLE
    }

    fun isDndEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        return when (notificationManager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> true
            else -> false
        }
    }


    private fun enableDND() {
        // Check if the app has permission to modify DND settings
        if (notificationManager.isNotificationPolicyAccessGranted) {
            dndSwitch.isChecked=true
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)

            val telephonyManager = requireActivity().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    super.onCallStateChanged(state, phoneNumber)
                    val isDNDActive = notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
                    if (isDNDActive && state == TelephonyManager.CALL_STATE_RINGING) {
                        // End the call
                        endCall()
                    }
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)

        } else {
            Toast.makeText(requireContext(), "Kindly Grant the DND permission for this app!", Toast.LENGTH_SHORT).show()
            // If not, request permission from the user
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivityForResult(intent, 1)
        }
    }

    //end call
    fun endCall() {
        try {
            val telephonyService = requireActivity().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val endCallMethod = telephonyService.javaClass.getDeclaredMethod("endCall")
            endCallMethod.isAccessible = true
            endCallMethod.invoke(telephonyService)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun disableDND() {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            dndSwitch.isChecked=false
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

}