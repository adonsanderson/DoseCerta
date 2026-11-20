# Plano de Tarefas (Roadmap SDD) — DoseCerta

> **Projeto:** DoseCerta (`dose-certa-android`)  
> **Metodologia:** Specification-Driven Development (SDD)  
> **Estratégia:** Desenvolvimento incremental em 6 Marcos sequenciais com validação contínua por testes unitários.

---

## Marco 1 — Fundação, Documentação SDD e Setup Base
Objetivo: Consolidar toda a especificação técnica, decisões arquiteturais, planejamento e configuração inicial do repositório.

- [x] Criar `docs/SPECIFICATION.md` detalhando visão geral, API OpenFDA, RF01–RF07, RNF01–RNF05 e regras de negócio.
- [x] Criar `docs/ARCHITECTURE.md` detalhando Clean Architecture, Regra de Dependência, UDF, Offline-First, AlarmManager e estrutura de pacotes (`com.example.dosecerta`).
- [x] Criar `docs/TASKS.md` com decomposição sequencial em 6 Marcos em formato checklist.
- [x] Criar `README.md` (Versão Inicial) com todas as seções obrigatórias do projeto.
- [ ] Configurar `gradle/libs.versions.toml` e `app/build.gradle.kts` com as dependências finais: Jetpack Compose + Material 3, Navigation Compose, Coroutines/StateFlow, Room + KSP, Retrofit + OkHttp + Moshi/Serialization, Koin (`koin-android`, `koin-androidx-compose`), JUnit 4, MockK, Turbine e `kotlinx-coroutines-test`.
- [ ] Estruturar os pacotes base sob `com.example.dosecerta` e validar compilação inicial do projeto.

---

## Marco 2 — Camada de Domínio (100% Kotlin Puro) e Testes Unitários
Objetivo: Implementar todas as entidades, contratos e regras de negócio sem nenhuma dependência de Android, Room ou Retrofit, garantindo alta cobertura de testes.

- [ ] Criar modelos de domínio imutáveis (`Treatment`, `Dose`, `DoseStatus`, `TreatmentWithDoses`, `AdherenceSummary`, `DrugInfo`, `DrugSearchResult`).
- [ ] Definir contratos de repositório (`TreatmentRepository`, `DrugInfoRepository`) e contratos de infraestrutura (`DoseAlarmScheduler`, `TimeProvider`).
- [ ] Implementar `CalculateDoseScheduleUseCase` (algoritmo puro de geração de cronograma a partir de `startTimeMillis`, `intervalHours` e `durationDays`, cobrindo validações e ordenação cronológica).
- [ ] Implementar `CreateTreatmentUseCase` (validação de entrada, cálculo das doses, persistência e agendamento de alarmes futuros).
- [ ] Implementar `ToggleDoseStatusUseCase` (alternância `TAKEN`/`PENDING`, registro de `takenAtMillis`, cancelamento/reagendamento do alarme).
- [ ] Implementar `DeleteTreatmentUseCase`, `ObserveTreatmentsUseCase`, `ObserveTodayDosesUseCase`, `ObserveTreatmentDetailUseCase` e `ReschedulePendingAlarmsUseCase`.
- [ ] Implementar `SearchDrugInfoUseCase` e `ObserveCachedDrugsUseCase`.
- [ ] Escrever testes unitários exaustivos para `CalculateDoseScheduleUseCaseTest` (JUnit 4): intervalos de 4h/6h/8h/12h/24h, durações variadas, virada de dia/mês/ano bissexto, intervalo maior que duração e entradas inválidas.
- [ ] Escrever testes unitários para `CreateTreatmentUseCaseTest` e `ToggleDoseStatusUseCaseTest` (JUnit 4 + MockK), verificando chamadas ao repositório e ao `DoseAlarmScheduler`.
- [ ] Executar `gradle :app:testDebugUnitTest` e confirmar 100% de aprovação na camada de domínio.

---

## Marco 3 — Camada de Dados Offline-First (Room + OpenFDA Retrofit) e Testes
Objetivo: Construir a persistência local como Single Source of Truth e a integração resiliente com a API REST OpenFDA com cache automático.

- [ ] Implementar entidades Room (`TreatmentEntity`, `DoseEntity` com chave estrangeira `CASCADE`, `DrugInfoEntity`) e `DoseCertaDatabase`.
- [ ] Implementar DAOs reativos (`TreatmentDao`, `DoseDao`, `DrugInfoDao`) com queries ordenadas, filtros de doses pendentes/do dia e busca textual no cache local.
- [ ] Implementar DTOs da API OpenFDA (`OpenFdaResponseDto`, `DrugLabelDto`, `OpenFdaMetaDto`) e interface `OpenFdaApiService` (`GET drug/label.json`).
- [ ] Implementar mappers puros (`TreatmentMapper`, `DoseMapper`, `DrugInfoMapper`) com normalização segura de listas/campos nulos da OpenFDA.
- [ ] Implementar `TreatmentRepositoryImpl` garantindo atomicidade na gravação de tratamento + doses.
- [ ] Implementar `DrugInfoRepositoryImpl` com estratégia Offline-First: consulta remota → upsert no Room → fallback automático para cache local em falhas de conectividade (`IOException`/timeout) e tratamento de `404` como lista vazia.
- [ ] Configurar módulos Koin `NetworkModule` e `DataModule`.
- [ ] Escrever testes unitários `TreatmentRepositoryImplTest` e `DrugInfoRepositoryImplTest` (JUnit 4 + MockK + Turbine) validando persistência SSOT, fallback offline, atualização de cache e mapeamento de erros HTTP/rede.
- [ ] Executar suíte de testes unitários e validar compilação limpa.

---

## Marco 4 — Despertador Local (`AlarmManager`, `BroadcastReceiver` e Notificações)
Objetivo: Garantir disparos sonoros confiáveis no horário exato da dose, mesmo com o aplicativo encerrado, tela apagada ou sem conexão à internet.

- [ ] Declarar permissões necessárias no `AndroidManifest.xml` (`INTERNET`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`) e registrar os `BroadcastReceiver`s.
- [ ] Implementar `NotificationHelper` com criação do canal `dose_reminders_channel` (`IMPORTANCE_HIGH`, som de alarme/notificação, vibração) e construção de notificações com título, nome do medicamento, horário, `PendingIntent` de abertura e ação rápida **"Marcar como Tomada"**.
- [ ] Implementar `AndroidDoseAlarmScheduler` utilizando `AlarmManager` (`setExactAndAllowWhileIdle` com checagem de `canScheduleExactAlarms()` para Android 12+ e `requestCode` determinístico baseado em `doseId`), incluindo métodos `scheduleDose`, `scheduleDoses`, `cancelDose` e `cancelDoses`.
- [ ] Implementar `DoseAlarmReceiver` para interceptar o alarme no horário exato e disparar a notificação sonora.
- [ ] Implementar `DoseActionReceiver` para permitir marcar a dose como `TAKEN` diretamente da barra de notificações usando `goAsync()` + Coroutines.
- [ ] Implementar `BootCompletedReceiver` para reagendar automaticamente todas as doses pendentes futuras após reinicialização do dispositivo.
- [ ] Registrar `AlarmModule` no Koin e validar fluxo de permissão de notificações (Android 13+) e alarmes exatos (Android 12+).

---

## Marco 5 — Camada de Apresentação (Jetpack Compose, M3, UDF) e Testes de ViewModel
Objetivo: Construir uma interface polida, responsiva, acessível e otimizada para recomposição, com tratamento explícito de `Loading`, `Success`, `Empty` e `Error`.

- [ ] Configurar tema Material 3 (`Color.kt`, `Type.kt`, `Shape.kt`, `Theme.kt`) com suporte a modo claro/escuro, `enableEdgeToEdge()` e ícone adaptativo customizado do app.
- [ ] Implementar componentes compartilhados (`LoadingView`, `EmptyStateView`, `ErrorStateView`, `DoseItemCard`, `AdherenceProgressCard`, `MedicalDisclaimerBanner`) com `Modifier.testTag` e `contentDescription`.
- [ ] Implementar tela **Home / Cronograma** (`HomeScreen` + `HomeViewModel`): resumo de adesão do dia, próximas doses, marcação rápida Tomada/Pendente, lista de tratamentos ativos e FAB para novo tratamento.
- [ ] Implementar tela **Cadastro de Tratamento** (`AddTreatmentScreen` + `AddTreatmentViewModel`): formulário com nome, dosagem, seletor de data/hora inicial, chips de intervalo rápido (4h, 6h, 8h, 12h, 24h) + campo customizado, duração em dias, prévia do total de doses calculadas em tempo real e validação inline.
- [ ] Implementar tela **Detalhes do Tratamento** (`TreatmentDetailScreen` + `TreatmentDetailViewModel`): linha do tempo completa de todas as doses geradas, progresso percentual, alternância Tomada/Pendente e opção de exclusão com diálogo de confirmação.
- [ ] Implementar tela **Consulta de Bulas (OpenFDA + Cache Offline)** (`DrugSearchScreen` + `DrugSearchViewModel` + `DrugDetailSheet`): barra de busca, banner de modo offline/cache, histórico de bulas salvas no Room, visualização completa de indicações/advertências/posologia/contraindicações e aviso clínico.
- [ ] Configurar `DoseCertaNavHost` com Navigation Compose e barra de navegação inferior/adaptativa entre Cronograma e Bulas.
- [ ] Auditar otimizações de recomposição (`@Immutable`, `key` em `LazyColumn`, `remember`, `derivedStateOf`, `collectAsStateWithLifecycle`).
- [ ] Escrever testes unitários para `HomeViewModelTest`, `AddTreatmentViewModelTest`, `TreatmentDetailViewModelTest` e `DrugSearchViewModelTest` (JUnit 4 + MockK + Turbine) verificando transições `Loading` → `Success` / `Empty` / `Error` e ações do usuário.

---

## Marco 6 — Integração Final, Validação Offline-First, Evidências e Documentação
Objetivo: Validar ponta a ponta todos os requisitos funcionais e não funcionais, consolidar evidências visuais e finalizar o repositório para entrega.

- [ ] Integrar `DoseCertaApplication` (inicialização Koin + canal de notificação) e `MainActivity` (solicitação graciosa de `POST_NOTIFICATIONS` + `DoseCertaNavHost`).
- [ ] Executar bateria completa de testes unitários (`gradle :app:testDebugUnitTest`) e garantir 0 falhas e build limpo (`gradle assembleDebug`).
- [ ] Validar manualmente todos os fluxos críticos (RF01 a RF07):
  - [ ] Cadastro de tratamento e conferência matemática das doses geradas.
  - [ ] Disparo real de alarme sonoro com app fechado e sem internet.
  - [ ] Marcação de dose como Tomada (no app e via ação rápida da notificação) e atualização da adesão.
  - [ ] Consulta na OpenFDA com rede ativa e reabertura da mesma bula em Modo Avião.
  - [ ] Verificação visual dos estados `Loading`, `Success`, `Empty` e `Error`.
- [ ] Capturar screenshots e/ou gravações de tela das principais telas e estados para a seção de Evidências.
- [ ] Atualizar `README.md` com as evidências finais, resumo da cobertura de testes e revisão de limitações conhecidas.
- [ ] Revisar todos os arquivos em `docs/` garantindo consistência total entre documentação SDD e código entregue.
