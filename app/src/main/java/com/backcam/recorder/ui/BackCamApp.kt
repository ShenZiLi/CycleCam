package com.backcam.recorder.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.backcam.recorder.ui.navigation.Routes
import com.backcam.recorder.ui.screens.previewScreen
import com.backcam.recorder.ui.screens.galleryScreen
import com.backcam.recorder.ui.screens.settingsScreen

@Composable
fun BackCamApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.PREVIEW
    ) {
        previewScreen(
            onNavigateToGallery = { navController.navigate(Routes.GALLERY) },
            onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
        )
        galleryScreen(
            onNavigateBack = { navController.popBackStack() }
        )
        settingsScreen(
            onNavigateBack = { navController.popBackStack() }
        )
    }
}