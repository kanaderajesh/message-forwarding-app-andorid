package com.smsforwarder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.smsforwarder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private lateinit var adapter: KeywordsAdapter
    private val keywordsList = mutableListOf<String>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            startForwardingService()
        } else {
            Toast.makeText(this, "SMS permissions are required to forward messages.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        setupRecyclerView()
        loadSavedData()
        setupListeners()
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun setupRecyclerView() {
        adapter = KeywordsAdapter(keywordsList) { keyword ->
            prefs.removeKeyword(keyword)
            keywordsList.remove(keyword)
            adapter.notifyDataSetChanged()
            refreshEmptyState()
        }
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerKeywords.layoutManager = layoutManager
        binding.recyclerKeywords.addItemDecoration(
            DividerItemDecoration(this, layoutManager.orientation)
        )
        binding.recyclerKeywords.adapter = adapter
    }

    private fun loadSavedData() {
        binding.editTextForwardTo.setText(prefs.forwardToNumber)
        keywordsList.clear()
        keywordsList.addAll(prefs.getKeywords().sorted())
        adapter.notifyDataSetChanged()
        refreshEmptyState()
    }

    private fun setupListeners() {
        binding.buttonSaveNumber.setOnClickListener {
            val number = binding.editTextForwardTo.text.toString().trim()
            if (number.isEmpty()) {
                Toast.makeText(this, "Enter a phone number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.forwardToNumber = number
            Toast.makeText(this, "Forward-to number saved.", Toast.LENGTH_SHORT).show()
        }

        binding.buttonAddKeyword.setOnClickListener {
            val keyword = binding.editTextKeyword.text.toString().trim().lowercase()
            if (keyword.isEmpty()) {
                Toast.makeText(this, "Enter a keyword.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (keywordsList.contains(keyword)) {
                Toast.makeText(this, "Keyword already added.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.addKeyword(keyword)
            keywordsList.add(keyword)
            keywordsList.sort()
            adapter.notifyDataSetChanged()
            binding.editTextKeyword.text?.clear()
            refreshEmptyState()
        }

        binding.buttonStartService.setOnClickListener {
            val number = prefs.forwardToNumber.ifBlank {
                binding.editTextForwardTo.text.toString().trim()
            }
            if (number.isEmpty()) {
                Toast.makeText(this, "Set a forward-to number first.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (keywordsList.isEmpty()) {
                Toast.makeText(this, "Add at least one keyword first.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            prefs.forwardToNumber = number
            checkPermissionsAndStart()
        }

        binding.buttonStopService.setOnClickListener {
            stopForwardingService()
        }
    }

    private fun checkPermissionsAndStart() {
        val required = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            startForwardingService()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun startForwardingService() {
        startForegroundService(Intent(this, SmsForwardingService::class.java))
        refreshStatus()
        Toast.makeText(this, "SMS forwarding started.", Toast.LENGTH_SHORT).show()
    }

    private fun stopForwardingService() {
        stopService(Intent(this, SmsForwardingService::class.java))
        prefs.isForwardingEnabled = false
        refreshStatus()
        Toast.makeText(this, "SMS forwarding stopped.", Toast.LENGTH_SHORT).show()
    }

    private fun refreshStatus() {
        val active = prefs.isForwardingEnabled
        binding.textViewStatus.text = if (active) "Status: Active" else "Status: Stopped"
        binding.textViewStatus.setTextColor(
            getColor(if (active) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
        binding.buttonStartService.isEnabled = !active
        binding.buttonStopService.isEnabled = active
    }

    private fun refreshEmptyState() {
        binding.textNoKeywords.visibility =
            if (keywordsList.isEmpty()) View.VISIBLE else View.GONE
    }
}
