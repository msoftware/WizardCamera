package com.shevelev.wizard_camera.application.di

import com.shevelev.wizard_camera.gallery_activity.di.GalleryActivityComponent
import com.shevelev.wizard_camera.main_activity.di.MainActivityComponent
import dagger.Module

@Module(subcomponents = [
    MainActivityComponent::class,
    GalleryActivityComponent::class
])
class AppModuleChilds