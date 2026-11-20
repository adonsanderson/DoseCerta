# Arquitetura de Software — DoseCerta

> **Projeto:** DoseCerta (`dose-certa-android`)  
> **Pacote Base:** `com.example.dosecerta`  
> **Padrão Arquitetural:** Clean Architecture + MVVM + UDF (Unidirectional Data Flow)  
> **Persistência:** Offline-First com Room (Single Source of Truth)

---

## 1. Visão Geral da Clean Architecture

O **DoseCerta** é estruturado segundo os princípios da **Clean Architecture**, promovendo baixo acoplamento, alta coesão, independência de frameworks na lógica de negócio e máxima testabilidade na JVM.

A aplicação é dividida em três camadas principais — **Presentation**, **Domain** e **Data** — complementadas por um módulo de infraestrutura de sistema para agendamento de alarmes (**`alarm/`**) e pela camada de composição de dependências (**`di/`**).

```text
┌──────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                           │
│   Jetpack Compose (Screens, Components) · Navigation · ViewModels    │
│   UiState (Loading / Success / Empty / Error) · StateFlow · Events   │
└──────────────────────────────────┬───────────────────────────────────┘
                                   │ depende de
                                   ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                               │
│                     (100% KOTLIN PURO - JVM)                         │
│   Models (Treatment, Dose, DrugInfo, DoseStatus)                     │
│   UseCases (CalculateDoseScheduleUseCase, CreateTreatmentUseCase...) │
│   Interfaces / Ports (TreatmentRepository, DrugInfoRepository,       │
│                       DoseAlarmScheduler)                            │
└───────────────▲──────────────────────────────────────▲───────────────┘
                │ implementa                           │ implementa
┌───────────────┴──────────────────┐  ┌────────────────┴───────────────┐
│            DATA LAYER            │  │          ALARM MODULE          │
│  Room (DB, Entities, DAOs)       │  │  AndroidDoseAlarmScheduler     │
│  Retrofit + OkHttp (OpenFDA API) │  │  DoseAlarmReceiver             │
│  DTOs · Mappers                  │  │  DoseActionReceiver            │
│  Repository Implementations      │  │  BootCompletedReceiver         │
│  (Offline-First / SSOT)          │  │  NotificationHelper            │
└──────────────────────────────────┘  └────────────────────────────────┘
```

---

## 2. Regra de Dependência (Dependency Rule)

A **Regra de Dependência** é rigorosamente aplicada em todo o projeto:

> **Dependências de código-fonte devem apontar exclusivamente de fora para dentro (em direção à camada `Domain`).**

### 2.1 Pureza da Camada `Domain` (100% Kotlin Puro)
- A camada `Domain` **não conhece** Jetpack Compose, Android SDK (`android.*`), Room (`androidx.room.*`), Retrofit (`retrofit2.*`), OkHttp ou Koin.
- O algoritmo central de cálculo do cronograma de doses (`CalculateDoseScheduleUseCase`) opera estritamente com tipos nativos da linguagem Kotlin (`Long`, `Int`, `String`, `List`, classes de domínio imutáveis), sem depender de `Calendar`, `Context`, `SystemClock` ou banco de dados.
- Para permitir testes 100% determinísticos de regras dependentes de data/hora atual (ex.: verificar se uma dose está atrasada ou no futuro para agendamento), o domínio utiliza abstrações ou injeção do relógio (`TimeProvider` / parâmetro `nowMillis: Long`).

### 2.2 Inversão de Dependência (DIP)
- **`Domain` define as interfaces (contratos):**
  - `TreatmentRepository`
  - `DrugInfoRepository`
  - `DoseAlarmScheduler`
- **`Data` implementa `TreatmentRepository` e `DrugInfoRepository`** utilizando Room e Retrofit.
- **`alarm/` implementa `DoseAlarmScheduler`** utilizando `AlarmManager` e `PendingIntent` do Android.
- **`di/` (Koin)** conecta cada interface à sua implementação concreta em tempo de inicialização, mantendo o acoplamento isolado no ponto de composição.

---

## 3. Responsabilidades por Camada

### 3.1 Camada `Domain` (`com.example.dosecerta.domain`)
Contém o núcleo de negócio independente de plataforma:
- **`model/`**: Entidades de domínio imutáveis (`Treatment`, `Dose`, `DoseStatus`, `TreatmentWithDoses`, `DrugInfo`, `AdherenceSummary`).
- **`repository/`**: Contratos de acesso a dados (`TreatmentRepository`, `DrugInfoRepository`).
- **`scheduler/`**: Contrato de agendamento de alarmes (`DoseAlarmScheduler`) e provedor de tempo (`TimeProvider`).
- **`usecase/`**: Regras de negócio encapsuladas em casos de uso de responsabilidade única:
  - `CalculateDoseScheduleUseCase`: Algoritmo puro que recebe data/hora inicial, intervalo em horas e duração em dias e retorna a lista cronológica de doses calculadas.
  - `CreateTreatmentUseCase`: Valida os dados, calcula as doses via `CalculateDoseScheduleUseCase`, persiste tratamento + doses no repositório e agenda os alarmes futuros via `DoseAlarmScheduler`.
  - `ObserveTreatmentsUseCase` / `ObserveTodayDosesUseCase` / `ObserveTreatmentDetailUseCase`: Expõem fluxos reativos com cálculo de adesão e classificação de doses atrasadas.
  - `ToggleDoseStatusUseCase`: Alterna status entre `TAKEN` e `PENDING`, atualiza `takenAtMillis` e cancela/reagenda o alarme correspondente.
  - `DeleteTreatmentUseCase`: Cancela todos os alarmes do tratamento e remove os registros do banco.
  - `SearchDrugInfoUseCase` / `ObserveCachedDrugsUseCase`: Orquestram busca na API OpenFDA com persistência em cache offline.
  - `ReschedulePendingAlarmsUseCase`: Recupera todas as doses futuras pendentes e reenvia ao `DoseAlarmScheduler` (acionado pós-boot).

### 3.2 Camada `Data` (`com.example.dosecerta.data`)
Responsável por persistência local, comunicação HTTP e mapeamento de modelos:
- **`local/` (Room)**:
  - `DoseCertaDatabase`: Instância do banco SQLite (`version = 1`) contendo as tabelas `treatments`, `doses` e `drug_info_cache`.
  - `entity/`: `TreatmentEntity`, `DoseEntity` (com `@ForeignKey` em `treatmentId` e `onDelete = CASCADE`), `DrugInfoEntity`.
  - `dao/`: `TreatmentDao`, `DoseDao`, `DrugInfoDao` retornando `Flow<List<...>>` para reatividade contínua e funções `suspend` para escrita em transações (`@Transaction`).
- **`remote/` (Retrofit + OkHttp + Moshi/Serialization)**:
  - `OpenFdaApiService`: Interface Retrofit para `GET drug/label.json`.
  - `dto/`: `OpenFdaResponseDto`, `DrugLabelDto`, `OpenFdaMetaDto` representando fielmente o JSON retornado pela FDA, tratando campos opcionais/listas vazias de forma segura.
- **`mapper/`**: Funções puras de extensão (`toDomain()`, `toEntity()`) isolando DTOs de rede e Entities do Room dos modelos de domínio.
- **`repository/`**:
  - `TreatmentRepositoryImpl`: Orquestra operações transacionais entre `TreatmentDao` e `DoseDao`.
  - `DrugInfoRepositoryImpl`: Implementa a política Offline-First entre `OpenFdaApiService` e `DrugInfoDao`.

### 3.3 Módulo de Alarmes (`com.example.dosecerta.alarm`)
Isola toda a complexidade de APIs do sistema Android para despertador local:
- **`AndroidDoseAlarmScheduler`**: Implementação de `DoseAlarmScheduler`. Utiliza `AlarmManager.setExactAndAllowWhileIdle(...)` (com verificação de `canScheduleExactAlarms()` na API 31+ e fallback seguro para `setAndAllowWhileIdle` caso o usuário negue a permissão exata), garantindo disparo mesmo em modo Doze.
- **`DoseAlarmReceiver`** (`BroadcastReceiver`): Acionado no horário exato da dose; extrai `doseId`, nome do medicamento, dosagem e horário da `Intent` e delega ao `NotificationHelper`.
- **`DoseActionReceiver`** (`BroadcastReceiver`): Trata o clique no botão de ação rápida **"Marcar como Tomada"** diretamente da notificação, atualizando o banco Room via `goAsync()` + Coroutine e dispensando a notificação.
- **`BootCompletedReceiver`** (`BroadcastReceiver`): Escuta `android.intent.action.BOOT_COMPLETED` (e `ACTION_LOCKED_BOOT_COMPLETED` quando aplicável) para invocar `ReschedulePendingAlarmsUseCase`, evitando perda de alarmes após reinicialização do aparelho.
- **`NotificationHelper`**: Cria o canal de notificação `dose_reminders_channel` (`IMPORTANCE_HIGH`, som de alarme/notificação, vibração) e constrói notificações acessíveis com `PendingIntent` para abrir o aplicativo e ação rápida de confirmação.

### 3.4 Camada `Presentation` (`com.example.dosecerta.presentation`)
Implementa a interface do usuário com Jetpack Compose, Material 3 e ViewModels:
- **`navigation/`**: Grafo de navegação (`DoseCertaNavHost`) e rotas tipadas (`DoseCertaRoute`: `Home`, `AddTreatment`, `TreatmentDetail(treatmentId)`, `DrugSearch`, `DrugDetail(drugId)`).
- **`theme/`**: Paleta de cores M3 (`Color.kt`), tipografia (`Type.kt`), formas e tema dinâmico/customizado (`Theme.kt`).
- **`components/`**: Componentes reutilizáveis (`LoadingView`, `EmptyStateView`, `ErrorStateView`, `DoseCard`, `AdherenceProgressBar`, `MedicalDisclaimerBanner`).
- **`screens/`**: Telas agrupadas por funcionalidade (`home/`, `addtreatment/`, `treatmentdetail/`, `drugsearch/`), cada uma com seu `UiState`, `UiEvent` (quando aplicável), `ViewModel` e Composables sem estado (*stateless*) para facilitar previews e evitar recomposições.

---

## 4. Fluxo de Dados Unidirecional (UDF)

Todas as telas do **DoseCerta** seguem o padrão **Unidirectional Data Flow (UDF)**:

```text
      ┌─────────────────────────────────────────────────────┐
      │                                                     │
      ▼                                                     │
┌───────────────┐   1. Ação do Usuário (Event/Callback)     │
│  Compose UI   ├──────────────────────────────────────────►│
│  (Stateless)  │                                           ▼
└───────▲───────┘                                    ┌─────────────┐
        │                                            │  ViewModel  │
        │ 4. Renderização Reativa                    └──────┬──────┘
        │    collectAsStateWithLifecycle()                  │
        │                                                   │ 2. Executa UseCase
┌───────┴───────┐                                           ▼
│   StateFlow   │◄──────── 3. Emissão de Novo UiState ── Domain / Data
│  <UiState>    │           (Loading / Success /         (Room Flow / API)
└───────────────┘            Empty / Error)
```

### 4.1 Modelagem Explícita de Estados (RF07)

Cada tela utiliza um contrato de estado selado (`sealed interface`) ou `data class` com sub-estado explícito garantindo os quatro estados obrigatórios:

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String) : UiState<Nothing>
    data class Error(val message: String, val canRetry: Boolean = true) : UiState<Nothing>
}
```

- **Benefícios:** Elimina estados impossíveis (ex.: exibir loading e erro ao mesmo tempo), simplifica a UI Compose com `when (uiState)` exaustivo e torna os testes com **Turbine** diretos e previsíveis.

---

## 5. Estratégia Offline-First (Room como Single Source of Truth)

O **DoseCerta** adota o **Room (SQLite)** como única fonte de verdade (*Single Source of Truth — SSOT*) para toda a aplicação.

### 5.1 Fluxo de Tratamentos e Doses (100% Local)
1. O usuário preenche o formulário de tratamento e aciona "Salvar".
2. `CreateTreatmentUseCase` calcula todas as doses em memória (Kotlin puro).
3. `TreatmentRepositoryImpl` executa uma transação atômica no Room inserindo o `TreatmentEntity` e todos os `DoseEntity` vinculados, retornando os IDs gerados.
4. `DoseAlarmScheduler` agenda os alarmes exatos no `AlarmManager` usando os IDs persistidos.
5. Os `Flow`s expostos pelo `TreatmentDao` e `DoseDao` emitem automaticamente a lista atualizada para os ViewModels, atualizando a UI sem chamadas manuais de refresh.

### 5.2 Fluxo de Consulta de Bulas OpenFDA (Remote + Cache Local)
```text
Usuário pesquisa "Ibuprofen"
        │
        ▼
DrugInfoRepositoryImpl.searchDrugs(query)
        │
        ├──► 1. Tenta requisição GET /drug/label.json via Retrofit
        │         │
        │         ├─ Sucesso (200 OK com resultados):
        │         │     • Converte DTO → DrugInfoEntity (com query + cachedAtMillis)
        │         │     • Faz upsert no Room (DrugInfoDao)
        │         │     • Retorna dados atualizados (isFromCache = false)
        │         │
        │         ├─ Sem resultados na API (404 / lista vazia):
        │         │     • Verifica se há resultados compatíveis no cache local
        │         │     • Se não houver, retorna estado Empty
        │         │
        │         └─ Falha de rede / Sem internet (IOException / Timeout):
        │               • Consulta fallback no Room (DrugInfoDao.searchCached(query))
        │               • Se encontrar no cache → retorna Success(isFromCache = true)
        │               • Se não encontrar → retorna Error amigável com ação "Tentar novamente"
        │
        └──► 2. Tela inicial de Bulas observa Flow<List<DrugInfo>> do Room
                • Histórico completo sempre disponível offline instantaneamente
```

---

## 6. Injeção de Dependências com Koin

A configuração do Koin em `com.example.dosecerta.di` é dividida em módulos especializados:

| Módulo Koin | Componentes Registrados |
| :--- | :--- |
| `networkModule` | `OkHttpClient` (timeouts + `HttpLoggingInterceptor`), conversor JSON, `Retrofit` (`https://api.fda.gov/`), `OpenFdaApiService`. |
| `dataModule` | `DoseCertaDatabase`, `TreatmentDao`, `DoseDao`, `DrugInfoDao`, `TreatmentRepositoryImpl` (bind `TreatmentRepository`), `DrugInfoRepositoryImpl` (bind `DrugInfoRepository`). |
| `alarmModule` | `NotificationHelper`, `AndroidDoseAlarmScheduler` (bind `DoseAlarmScheduler`). |
| `domainModule` | `TimeProvider` (implementação padrão do sistema), `CalculateDoseScheduleUseCase`, `CreateTreatmentUseCase`, `ObserveTreatmentsUseCase`, `ObserveTodayDosesUseCase`, `ObserveTreatmentDetailUseCase`, `ToggleDoseStatusUseCase`, `DeleteTreatmentUseCase`, `SearchDrugInfoUseCase`, `ObserveCachedDrugsUseCase`, `ReschedulePendingAlarmsUseCase`. |
| `presentationModule` | `HomeViewModel`, `AddTreatmentViewModel`, `TreatmentDetailViewModel`, `DrugSearchViewModel`. |

---

## 7. Estrutura Completa de Pacotes Planejada

```text
app/src/main/java/com/example/dosecerta/
├── DoseCertaApplication.kt
├── MainActivity.kt
│
├── di/
│   ├── AlarmModule.kt
│   ├── DataModule.kt
│   ├── DomainModule.kt
│   ├── NetworkModule.kt
│   └── PresentationModule.kt
│
├── domain/                                 # 100% KOTLIN PURO (zero imports android.*)
│   ├── model/
│   │   ├── AdherenceSummary.kt
│   │   ├── Dose.kt
│   │   ├── DoseStatus.kt
│   │   ├── DrugInfo.kt
│   │   ├── DrugSearchResult.kt
│   │   ├── Treatment.kt
│   │   └── TreatmentWithDoses.kt
│   ├── repository/
│   │   ├── DrugInfoRepository.kt
│   │   └── TreatmentRepository.kt
│   ├── scheduler/
│   │   ├── DoseAlarmScheduler.kt
│   │   └── TimeProvider.kt
│   └── usecase/
│       ├── CalculateDoseScheduleUseCase.kt
│       ├── CreateTreatmentUseCase.kt
│       ├── DeleteTreatmentUseCase.kt
│       ├── ObserveCachedDrugsUseCase.kt
│       ├── ObserveTodayDosesUseCase.kt
│       ├── ObserveTreatmentDetailUseCase.kt
│       ├── ObserveTreatmentsUseCase.kt
│       ├── ReschedulePendingAlarmsUseCase.kt
│       ├── SearchDrugInfoUseCase.kt
│       └── ToggleDoseStatusUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── DoseCertaDatabase.kt
│   │   ├── dao/
│   │   │   ├── DoseDao.kt
│   │   │   ├── DrugInfoDao.kt
│   │   │   └── TreatmentDao.kt
│   │   └── entity/
│   │       ├── DoseEntity.kt
│   │       ├── DrugInfoEntity.kt
│   │       └── TreatmentEntity.kt
│   ├── remote/
│   │   ├── OpenFdaApiService.kt
│   │   └── dto/
│   │       ├── DrugLabelDto.kt
│   │       ├── OpenFdaMetaDto.kt
│   │       └── OpenFdaResponseDto.kt
│   ├── mapper/
│   │   ├── DoseMapper.kt
│   │   ├── DrugInfoMapper.kt
│   │   └── TreatmentMapper.kt
│   └── repository/
│       ├── DrugInfoRepositoryImpl.kt
│       └── TreatmentRepositoryImpl.kt
│
├── alarm/
│   ├── AndroidDoseAlarmScheduler.kt
│   ├── BootCompletedReceiver.kt
│   ├── DoseActionReceiver.kt
│   ├── DoseAlarmReceiver.kt
│   └── NotificationHelper.kt
│
└── presentation/
    ├── common/
    │   └── UiState.kt
    ├── components/
    │   ├── AdherenceProgressCard.kt
    │   ├── DoseItemCard.kt
    │   ├── EmptyStateView.kt
    │   ├── ErrorStateView.kt
    │   ├── LoadingView.kt
    │   └── MedicalDisclaimerBanner.kt
    ├── navigation/
    │   ├── DoseCertaNavHost.kt
    │   └── DoseCertaRoute.kt
    ├── theme/
    │   ├── Color.kt
    │   ├── Shape.kt
    │   ├── Theme.kt
    │   └── Type.kt
    └── screens/
        ├── home/
        │   ├── HomeScreen.kt
        │   ├── HomeUiState.kt
        │   └── HomeViewModel.kt
        ├── addtreatment/
        │   ├── AddTreatmentScreen.kt
        │   ├── AddTreatmentUiState.kt
        │   └── AddTreatmentViewModel.kt
        ├── treatmentdetail/
        │   ├── TreatmentDetailScreen.kt
        │   ├── TreatmentDetailUiState.kt
        │   └── TreatmentDetailViewModel.kt
        └── drugsearch/
            ├── DrugSearchScreen.kt
            ├── DrugSearchUiState.kt
            ├── DrugSearchViewModel.kt
            └── DrugDetailSheet.kt

app/src/test/java/com/example/dosecerta/
├── domain/usecase/
│   ├── CalculateDoseScheduleUseCaseTest.kt
│   ├── CreateTreatmentUseCaseTest.kt
│   └── ToggleDoseStatusUseCaseTest.kt
├── data/repository/
│   ├── DrugInfoRepositoryImplTest.kt
│   └── TreatmentRepositoryImplTest.kt
└── presentation/screens/
    ├── addtreatment/AddTreatmentViewModelTest.kt
    ├── drugsearch/DrugSearchViewModelTest.kt
    ├── home/HomeViewModelTest.kt
    └── treatmentdetail/TreatmentDetailViewModelTest.kt
```
