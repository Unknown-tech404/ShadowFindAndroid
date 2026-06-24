package com.example.shadowfind

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shadowfind.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var shadowFindService: ShadowFindService
    private var isScanning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        shadowFindService = ShadowFindService(this)
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.apply {
            btnScan.setOnClickListener {
                if (!isScanning) {
                    startScan()
                } else {
                    Toast.makeText(this@MainActivity, "Scan already in progress", Toast.LENGTH_SHORT).show()
                }
            }
            
            btnClear.setOnClickListener {
                clearResults()
            }
            
            // Setup RecyclerView
            rvResults.layoutManager = LinearLayoutManager(this@MainActivity)
            
            // Setup spinners for threads and depth
            val threadsOptions = arrayOf("5", "10", "15", "20", "30")
            val depthOptions = arrayOf("1", "2", "3", "4", "5")
            
            spinnerThreads.setOnItemSelectedListener { parent, view, position, id ->
                // Thread count selected
            }
            
            spinnerDepth.setOnItemSelectedListener { parent, view, position, id ->
                // Depth selected
            }
        }
    }
    
    private fun startScan() {
        val url = binding.etUrl.text.toString().trim()
        if (url.isEmpty()) {
            binding.etUrl.error = "Please enter a URL"
            return
        }
        
        val threads = binding.spinnerThreads.selectedItem.toString().toInt()
        val depth = binding.spinnerDepth.selectedItem.toString().toInt()
        
        isScanning = true
        binding.btnScan.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Scanning... Please wait"
        
        lifecycleScope.launch {
            try {
                // Show authentication dialog
                showAuthenticationDialog { username, password ->
                    val result = withContext(Dispatchers.IO) {
                        shadowFindService.scanWebsite(url, threads, depth, username, password)
                    }
                    
                    if (result.success) {
                        showResults(result.data)
                    } else {
                        showError(result.message)
                    }
                }
            } catch (e: Exception) {
                showError(e.message ?: "Unknown error occurred")
            } finally {
                isScanning = false
                binding.btnScan.isEnabled = true
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Ready"
            }
        }
    }
    
    private fun showAuthenticationDialog(onSuccess: (String, String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_auth, null)
        val etUsername = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etUsername)
        val etPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPassword)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Authentication Required")
            .setView(dialogView)
            .setPositiveButton("Authenticate") { dialog, _ ->
                val username = etUsername.text.toString()
                val password = etPassword.text.toString()
                if (username.isNotEmpty() && password.isNotEmpty()) {
                    onSuccess(username, password)
                } else {
                    Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                isScanning = false
                binding.btnScan.isEnabled = true
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Ready"
            }
            .show()
    }
    
    private fun showResults(data: ScanResult) {
        binding.tvStatus.text = "Scan Complete! Found ${data.totalLinks} links"
        
        val adapter = ResultsAdapter(data)
        binding.rvResults.adapter = adapter
        
        // Show summary in a card view
        binding.cardSummary.visibility = View.VISIBLE
        binding.tvSummary.text = """
            Total Links: ${data.totalLinks}
            Internal: ${data.internalCount}
            External: ${data.externalCount}
            Images: ${data.imageCount}
            Scripts: ${data.scriptCount}
            Emails: ${data.emailCount}
            Time: ${data.scanTime} seconds
        """.trimIndent()
        
        // Show success snackbar
        Snackbar.make(binding.root, "Scan completed successfully!", Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.green_700))
            .show()
    }
    
    private fun showError(message: String) {
        binding.tvStatus.text = "Error: $message"
        binding.progressBar.visibility = View.GONE
        binding.btnScan.isEnabled = true
        
        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.red_700))
            .show()
    }
    
    private fun clearResults() {
        binding.rvResults.adapter = null
        binding.cardSummary.visibility = View.GONE
        binding.etUrl.text?.clear()
        Toast.makeText(this, "Results cleared", Toast.LENGTH_SHORT).show()
    }
}
