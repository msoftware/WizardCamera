package com.shevelev.wizard_camera.main_activity.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.TextureView
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import com.shevelev.wizard_camera.R
import com.shevelev.wizard_camera.application.App
import com.shevelev.wizard_camera.camera.camera_renderer.CameraRenderer
import com.shevelev.wizard_camera.databinding.ActivityMainBinding
import com.shevelev.wizard_camera.gallery_activity.view.GalleryActivity
import com.shevelev.wizard_camera.main_activity.di.MainActivityComponent
import com.shevelev.wizard_camera.main_activity.dto.*
import com.shevelev.wizard_camera.main_activity.view.gestures.Gesture
import com.shevelev.wizard_camera.main_activity.view.gestures.GesturesDetector
import com.shevelev.wizard_camera.main_activity.view_model.MainActivityViewModel
import com.shevelev.wizard_camera.shared.dialogs.OkDialog
import com.shevelev.wizard_camera.shared.mvvm.view.ActivityBaseMVVM
import com.shevelev.wizard_camera.shared.mvvm.view_commands.ViewCommand
import com.shevelev.wizard_camera.shared.ui_utils.hideSystemUI
import kotlinx.android.synthetic.main.activity_main.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import kotlin.system.exitProcess

@RuntimePermissions
class MainActivity : ActivityBaseMVVM<ActivityMainBinding, MainActivityViewModel>() {
    private var renderer: CameraRenderer? = null
    private var textureView: TextureView? = null

    private lateinit var gestureDetector: GesturesDetector

    override fun provideViewModelType(): Class<MainActivityViewModel> = MainActivityViewModel::class.java

    override fun layoutResId(): Int = R.layout.activity_main

    override fun inject(key: String) = App.injections.get<MainActivityComponent>(key).inject(this)

    override fun releaseInjection(key: String) = App.injections.release<MainActivityComponent>(key)

    override fun linkViewModel(binding: ActivityMainBinding, viewModel: MainActivityViewModel) {
        binding.viewModel = viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        gestureDetector = GesturesDetector(this).apply { setOnGestureListener { processGesture(it) } }

        viewModel.selectedFilter.observe(this, Observer { renderer?.setSelectedFilter(it) })
        viewModel.allFiltersListData.observe(this, Observer { allFiltersCarousel.setStartData(it, viewModel) })
        viewModel.favoriteFiltersListData.observe(this, Observer { favoritesFiltersCarousel.setStartData(it, viewModel) })

        shootButton.setOnClickListener { textureView?.let { viewModel.onShootClick(it) } }
        flashButton.setOnClickListener { viewModel.onFlashClick() }
        filtersModeButton.setOnModeChangeListener { viewModel.onSwitchFilterModeClick(it) }
        autoFocusButton.setOnClickListener { viewModel.onAutoFocusClick() }
        expositionBar.setOnValueChangeListener { viewModel.onExposeValueUpdated(it) }
        galleryButton.setOnClickListener { viewModel.onGalleyClick() }

        allFiltersCarousel.setOnItemSelectedListener { viewModel.onFilterSelected(it) }
        favoritesFiltersCarousel.setOnItemSelectedListener { viewModel.onFavoriteFilterSelected(it) }

        settings.setOnSettingsChangeListener { viewModel.onFilterSettingsChange(it) }

        root.layoutTransition.setDuration(resources.getInteger(android.R.integer.config_shortAnimTime).toLong())
    }

    override fun onResume() {
        super.onResume()
        viewModel.onActive()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onInactive()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    override fun processViewCommand(command: ViewCommand) {
        when(command) {
            is SetupCameraCommand -> setupCameraWithPermissionCheck()
            is ReleaseCameraCommand -> releaseCamera()
            is SetFlashStateCommand -> renderer!!.updateFlashState(command.turnFlashOn)
            is ShowCapturingSuccessCommand -> captureSuccess.show(command.screenOrientation)
            is FocusOnTouchCommand -> renderer!!.focusOnTouch(command.touchPoint, command.touchAreaSize)
            is AutoFocusCommand -> renderer!!.setAutoFocus()
            is ZoomCommand -> renderer!!.zoom(command.touchDistance).let { viewModel.onZoomUpdated(it) }
            is ResetExposureCommand -> expositionBar.reset()
            is SetExposureCommand -> renderer!!.updateExposure(command.exposureValue)
            is NavigateToGalleryCommand -> navigateToGallery()
            is ExitCommand -> exit(command.messageResId)
            is ShowFilterSettingsCommand -> { settings.hide(); settings.show(command.settings) }
            is HideFilterSettingsCommand -> settings.hide()
        }
    }

    override fun onBackPressed() {
        if(viewModel.onBackClick()) {
            super.onBackPressed()
        }
    }

    @Suppress("MoveLambdaOutsideParentheses")
    @SuppressLint("ClickableViewAccessibility")
    @NeedsPermission(Manifest.permission.CAMERA)
    internal fun setupCamera() {
        if(!viewModel.isActive) {
            return
        }

        renderer = CameraRenderer(
            this,
            viewModel.isFlashActive,
            viewModel.isAutoFocus,
            viewModel.cameraSettings,
            { viewModel.onCameraIsSetUp() }).also {
                textureView = TextureView(this)
                root.addView(textureView, 0)
                textureView!!.surfaceTextureListener = it

                with(viewModel.cameraSettings.screenTextureSize) {
                    textureView!!.layoutParams = ConstraintLayout.LayoutParams(width, height)
                }

                textureView!!.setOnTouchListener { view, event ->
                    gestureDetector.onTouchEvent(view, event)
                    true
                }
                it.setSelectedFilter(viewModel.selectedFilter.value!!)
        }
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    internal fun onCameraPermissionsDenied() = viewModel.onPermissionDenied()

    @SuppressLint("ClickableViewAccessibility")
    private fun releaseCamera() {
        root.removeView(textureView)
        renderer = null

        textureView?.setOnTouchListener(null)
        textureView?.addOnLayoutChangeListener(null)

        textureView = null
    }

    private fun processGesture(gesture: Gesture) = viewModel.processGesture(gesture)

    private fun navigateToGallery() {
        val galleryIntent = Intent(this, GalleryActivity::class.java)
        startActivity(galleryIntent)
    }

    private fun exit(@StringRes messageResId: Int) {
        OkDialog.show(supportFragmentManager, messageResId) {
            exitProcess(0)
        }
    }
}