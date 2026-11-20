package com.example.dosecerta.di

import com.example.dosecerta.domain.scheduler.TimeProvider
import com.example.dosecerta.domain.usecase.CalculateDoseScheduleUseCase
import com.example.dosecerta.domain.usecase.CreateTreatmentUseCase
import com.example.dosecerta.domain.usecase.DeleteTreatmentUseCase
import com.example.dosecerta.domain.usecase.ObserveCachedDrugsUseCase
import com.example.dosecerta.domain.usecase.ObserveTodayDosesUseCase
import com.example.dosecerta.domain.usecase.ObserveTreatmentDetailUseCase
import com.example.dosecerta.domain.usecase.ObserveTreatmentsUseCase
import com.example.dosecerta.domain.usecase.ReschedulePendingAlarmsUseCase
import com.example.dosecerta.domain.usecase.SearchDrugInfoUseCase
import com.example.dosecerta.domain.usecase.ToggleDoseStatusUseCase
import org.koin.dsl.module

val domainModule = module {
    single<TimeProvider> { TimeProvider { System.currentTimeMillis() } }

    factory { CalculateDoseScheduleUseCase() }
    factory { CreateTreatmentUseCase(get(), get(), get(), get()) }
    factory { ToggleDoseStatusUseCase(get(), get(), get()) }
    factory { DeleteTreatmentUseCase(get(), get()) }
    factory { ObserveTreatmentsUseCase(get(), get()) }
    factory { ObserveTodayDosesUseCase(get()) }
    factory { ObserveTreatmentDetailUseCase(get(), get()) }
    factory { ReschedulePendingAlarmsUseCase(get(), get(), get()) }
    factory { SearchDrugInfoUseCase(get()) }
    factory { ObserveCachedDrugsUseCase(get()) }
}
