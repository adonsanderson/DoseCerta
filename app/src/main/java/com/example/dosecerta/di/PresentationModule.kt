package com.example.dosecerta.di

import com.example.dosecerta.presentation.screens.addtreatment.AddTreatmentViewModel
import com.example.dosecerta.presentation.screens.drugsearch.DrugSearchViewModel
import com.example.dosecerta.presentation.screens.home.HomeViewModel
import com.example.dosecerta.presentation.screens.treatmentdetail.TreatmentDetailViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    viewModel { AddTreatmentViewModel(get(), get(), get()) }
    viewModel { (treatmentId: Long) ->
        TreatmentDetailViewModel(
            treatmentId = treatmentId,
            observeTreatmentDetailUseCase = get(),
            toggleDoseStatusUseCase = get(),
            deleteTreatmentUseCase = get(),
            timeProvider = get()
        )
    }
    viewModel { DrugSearchViewModel(get(), get()) }
}
