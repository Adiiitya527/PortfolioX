package com.portfoliox.ui.common

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.portfoliox.PortfolioXApplication
import com.portfoliox.di.AppContainer

/**
 * Gives ViewModel factories access to the AppContainer.
 *
 * Why factories at all: Android creates ViewModels itself (so it can keep them alive across
 * screen rotation), which means we can't call their constructors directly. A factory tells
 * Android how to build one with its dependencies. CreationExtras carries context for that,
 * including the Application, where our AppContainer lives.
 *
 * Usage inside a ViewModel's companion object:
 * ```
 * val Factory = viewModelFactory {
 *     initializer { MyViewModel(appContainer.someRepository) }
 * }
 * ```
 */
val CreationExtras.appContainer: AppContainer
    get() = (checkNotNull(this[APPLICATION_KEY]) as PortfolioXApplication).container
