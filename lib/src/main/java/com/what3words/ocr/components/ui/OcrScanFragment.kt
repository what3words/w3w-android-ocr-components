package com.what3words.ocr.components.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.what3words.ocr.components.R
import com.what3words.ocr.components.databinding.FragmentOcrScanBinding
import com.what3words.ocr.components.models.OcrScanResult
import com.what3words.ocr.components.models.W3WOcrWrapper

@SuppressLint("UnsafeOptInUsageError")
class OcrScanFragment : Fragment(), OcrScanManager.OcrScanResultCallback {
    private lateinit var _binding: FragmentOcrScanBinding
    private var ocrScanResultCallback: OcrScanResultCallback? = null

    private val isAllPermissionsGranted
        get() = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                this.requireActivity(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    companion object {
        fun newInstance() = OcrScanFragment()
        internal val TAG = OcrScanFragment::class.java.name
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    interface OcrScanResultCallback {
        fun onFinished(result: OcrScanResult)
    }

    private var manager: OcrScanManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOcrScanBinding.inflate(inflater, container, false)
        initializeScan()
        return _binding.root
    }

    fun ocrWrapper(
        wrapper: W3WOcrWrapper,
        callback: OcrScanResultCallback
    ) {
        manager = OcrScanManager(wrapper, this)
        this.ocrScanResultCallback = callback
    }

    fun initializeScan() {
        if (isAllPermissionsGranted) {
            requireManager().startCamera(
                this.requireContext(), this, _binding.previewView
            )
            clearViews()
        } else {
            permReqLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    private fun clearViews() {
        _binding.cameraShutterFound.visibility = View.GONE
        _binding.cameraShutterActive.visibility = View.VISIBLE
        _binding.addressText.text = ""
        _binding.addressText.visibility = View.GONE
    }

    private val permReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requireManager().startCamera(
                    this.requireContext(),
                    this,
                    _binding.previewView
                )
                clearViews()
            } else {
                //show error message
            }
        }

    private fun requireManager(): OcrScanManager {
        return this.manager
            ?: throw IllegalStateException("Fragment missing setup.")
    }

    override fun onFinished(result: OcrScanResult) {
        if (result.suggestions.isNotEmpty()) {
            val topSuggestion = result.suggestions.minBy { it.rank }
            _binding.cameraShutterFound.visibility = View.VISIBLE
            _binding.cameraShutterActive.visibility = View.GONE
            _binding.addressText.text = getString(R.string.slashes, topSuggestion.words)
            _binding.addressText.visibility = View.VISIBLE
        }
        ocrScanResultCallback?.onFinished(result)
    }

    override fun onDestroy() {
        super.onDestroy()
        manager?.stop()
    }
}