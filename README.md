# DoseCerta

Aplicativo Android nativo **Offline-First** para gerenciamento de tratamentos medicamentosos, cálculo automático de cronograma de doses, despertador local com notificações sonoras (`AlarmManager` + `BroadcastReceiver`) e consulta de bulas/advertências farmacológicas via API REST pública **OpenFDA** com cache offline.

> **Repositório:** `dose-certa-android`  
> **Pacote:** `com.example.dosecerta`  
> **Status do Projeto:** v1.0 Concluída (Marcos 1 a 6 finalizados • 67/67 testes aprovados)

---

## 1. Objetivo

Aumentar a segurança e a adesão a tratamentos medicamentosos eliminando erros no cálculo manual de horários, garantindo que os lembretes sonoros sejam disparados pontualmente **mesmo sem acesso à internet ou com o aplicativo fechado**, e disponibilizando consulta rápida a advertências e informações farmacológicas com persistência local para leitura offline.

---

## 2. Funcionalidades Implementadas

- **Cadastro de Tratamentos (RF01):** Registro de medicamento, dosagem, observações, data/hora da primeira dose (com seletores Material 3 de data e hora), chips de intervalo rápido (4h, 6h, 8h, 12h, 24h) ou customizado, duração em dias e prévia em tempo real do total de doses.
- **Cálculo Automático de Cronograma (RF02):** Algoritmo determinístico 100% Kotlin puro (`CalculateDoseScheduleUseCase`) que gera todos os horários exatos ao longo do tratamento, lidando com viradas de dia, mês, ano bissexto e regra RN01.
- **Despertador e Notificações Locais (RF03):** Agendamento via `AlarmManager` (`setExactAndAllowWhileIdle` com verificação de `canScheduleExactAlarms()` no Android 12+ e fallback seguro) e `BroadcastReceiver` com notificação sonora de alta prioridade, ação rápida **"Marcar como tomada"** e reagendamento automático pós-boot (`BootCompletedReceiver`).
- **Controle de Adesão (RF04):** Alternância de cada dose entre **"Tomada"** e **"Pendente"**, destaque visual para doses atrasadas e cartões de progresso percentual de adesão do dia e por tratamento.
- **Consulta de Bulas via OpenFDA + Cache Offline (RF05):** Busca na API REST pública OpenFDA com sugestões rápidas, gravação automática no Room, histórico offline imediato, modal completo de detalhes farmacológicos e banner de aviso clínico.
- **Persistência Offline-First (RF06):** Banco SQLite via Room como *Single Source of Truth* para tratamentos, doses (com integridade referencial `CASCADE`) e cache de bulas.
- **Estados Visuais Explícitos (RF07):** Tratamento completo e consistente dos estados `Loading`, `Success`, `Empty` e `Error` em todas as telas, com layout adaptativo (`NavigationBar` em celulares e `NavigationRail` em telas largas) e tema claro/escuro Material 3.

---

## 3. Tecnologias e Bibliotecas

| Categoria | Tecnologia / Biblioteca |
| :--- | :--- |
| **Linguagem** | Kotlin |
| **Interface (UI)** | Jetpack Compose + Material Design 3 (`Outfit` + `Plus Jakarta Sans` locais) |
| **Navegação** | Navigation Compose (adaptativa com `NavigationBar` / `NavigationRail`) |
| **Concorrência & Estado** | Kotlin Coroutines, `Flow` e `StateFlow` (`collectAsStateWithLifecycle`) |
| **Persistência Local** | Room (SQLite) + KSP |
| **Rede (API REST)** | Retrofit + OkHttp + Moshi (codegen KSP) |
| **Injeção de Dependências** | Koin (`koin-android`, `koin-androidx-compose`) |
| **Alarmes & Notificações** | Android `AlarmManager`, `BroadcastReceiver`, `NotificationManagerCompat` |
| **Testes Automatizados** | JUnit 4, MockK, Turbine, `kotlinx-coroutines-test`, Robolectric + Compose UI Test |

---

## 4. API REST Utilizada (OpenFDA)

O aplicativo consome a **OpenFDA Drug Labeling API**, mantida pela *U.S. Food and Drug Administration (FDA)*:

- **Base URL:** `https://api.fda.gov/`
- **Endpoint:** `GET /drug/label.json`
- **Autenticação:** **Não requer API Key** (pública e gratuita).
- **Dados Utilizados:** Nome comercial (`openfda.brand_name`), princípio ativo (`openfda.generic_name`), fabricante, indicações de uso (`indications_and_usage`), advertências (`warnings` / `warnings_and_cautions`), posologia (`dosage_and_administration`), contraindicações (`contraindications`) e reações adversas (`adverse_reactions`).
- **Uso Offline-First:** Toda consulta bem-sucedida é persistida no banco Room para consulta offline imediata e fallback automático em caso de perda de conectividade.

> **Aviso:** As informações retornadas pela OpenFDA possuem caráter estritamente informativo e não substituem a orientação de um médico ou farmacêutico.

---

## 5. Arquitetura

O **DoseCerta** adota **Clean Architecture** combinada com **MVVM** e **Unidirectional Data Flow (UDF)**:

- **`domain/` (100% Kotlin Puro):** Modelos de domínio, interfaces (`TreatmentRepository`, `DrugInfoRepository`, `DoseAlarmScheduler`, `TimeProvider`) e 10 UseCases — sem nenhuma dependência de `android.*`, `androidx.*`, Room ou Retrofit.
- **`data/`:** Banco local Room (`DoseCertaDatabase`, Entities, DAOs), cliente Retrofit OpenFDA (`OpenFdaApiService`, DTOs Moshi), mappers e repositórios Offline-First.
- **`alarm/`:** `AndroidDoseAlarmScheduler`, `NotificationHelper`, `DoseAlarmReceiver`, `DoseActionReceiver` e `BootCompletedReceiver`.
- **`presentation/`:** Telas Jetpack Compose (`HomeScreen`, `AddTreatmentScreen`, `TreatmentDetailScreen`, `DrugSearchScreen`, `DrugDetailSheet`), `DoseCertaNavHost`, tema Material 3 e ViewModels com `StateFlow<UiState>`.
- **`di/`:** 5 módulos Koin (`networkModule`, `dataModule`, `alarmModule`, `domainModule`, `presentationModule`).

Para documentação técnica completa, consulte:
- [Especificação de Software (SDD)](docs/SPECIFICATION.md)
- [Arquitetura do Sistema](docs/ARCHITECTURE.md)
- [Plano de Tarefas e Marcos](docs/TASKS.md)

---

## 6. Estratégia Offline-First

O aplicativo utiliza o **Room (SQLite)** como **Single Source of Truth (SSOT)**:

1. **Tratamentos, Doses e Alarmes:** Operam **100% offline**. Ao criar um tratamento ou alternar o status de uma dose, a atualização é gravada em transação no Room, os alarmes são sincronizados no `AlarmManager` e a UI reage automaticamente aos `Flow`s do banco.
2. **Consulta de Bulas (OpenFDA):** Ao buscar um medicamento com internet, os dados retornados são armazenados em cache local no Room. Em cenários sem conectividade ou falha de rede, o aplicativo realiza fallback automático para os registros em cache e exibe o histórico completo de medicamentos consultados para leitura offline.

---

## 7. Instruções de Configuração e Execução

### Pré-requisitos
- **Android Studio** (Ladybug ou superior recomendado)
- **JDK 11+**
- **Android SDK** com API mínima `24` (Android 7.0) e alvo `36`
- Dispositivo físico Android ou Emulador (AVD)

### Passos para Executar
1. Clone o repositório:
   ```bash
   git clone https://github.com/<seu-usuario>/dose-certa-android.git
   cd dose-certa-android
   ```
2. Abra o projeto no Android Studio e aguarde a sincronização do Gradle.
3. **Nenhuma chave de API é necessária** — a OpenFDA Drug Labeling API funciona publicamente sem configuração adicional de segredos.
4. Selecione um dispositivo/emulador e clique em **Run (`app`)**, ou compile via terminal:
   ```bash
   ./gradlew assembleDebug
   ```
5. No primeiro uso em dispositivos Android 13+, conceda a permissão de **Notificações** (e, em Android 12+, permita **Alarmes e Lembretes** no banner da tela inicial) para garantir o disparo exato dos despertadores.

---

## 8. Instruções de Testes e Cobertura

A suíte automatizada conta com **67 testes aprovados** (JUnit 4, MockK, Turbine, `kotlinx-coroutines-test` e Robolectric + Compose UI Test), cobrindo todas as camadas:

| Camada | Suítes de Teste | Escopo Validado |
| :--- | :--- | :--- |
| **Domain** | `CalculateDoseScheduleUseCaseTest`, `CreateTreatmentUseCaseTest`, `ToggleDoseStatusUseCaseTest`, `DeleteTreatmentUseCaseTest`, `ObserveTreatmentsUseCaseTest`, `RescheduleAndSearchUseCasesTest` | Algoritmo de cálculo (4h–48h, viradas de dia/mês/ano bissexto, RN01, validações), persistência + agendamento de alarmes futuros, alternância Tomada/Pendente, adesão reativa e RN06 |
| **Data** | `TreatmentRepositoryImplTest`, `DrugInfoRepositoryImplTest` | Transação atômica Room, fluxos SSOT, mapeamento OpenFDA, sanitização de busca, cache offline-first, fallback em `IOException`/HTTP 500 e tratamento de HTTP 404 |
| **Alarm** | `AlarmConstantsTest`, `AndroidDoseAlarmSchedulerTest` | IDs determinísticos (RN05), agendamento exato e fallback no `AlarmManager`, extras de `PendingIntent` e cancelamento |
| **Presentation** | `HomeViewModelTest`, `AddTreatmentViewModelTest`, `TreatmentDetailViewModelTest`, `DrugSearchViewModelTest`, `DoseCertaCujTest` | Transições `Loading` / `Success` / `Empty` / `Error`, prévia em tempo real de doses, renderização Compose e jornadas críticas de usuário (CUJs) |

Para executar todos os testes:

```bash
./gradlew testDebugUnitTest
```

> Em ambientes sem wrapper (`gradlew`), utilize diretamente:
> ```bash
> gradle :app:testDebugUnitTest
> ```

Relatório HTML gerado em:  
`app/build/reports/tests/testDebugUnitTest/index.html`

---

## 9. Evidências

| Fluxo / Validação | Status |
| :--- | :--- |
| Suíte automatizada completa (67/67 testes unitários + Robolectric + Compose UI) | Validado (`BUILD SUCCESSFUL`) |
| Home — Cronograma Diário, Adesão e Estados `Loading` / `Empty` / `Error` | Validado via `HomeViewModelTest` + `DoseCertaCujTest` |
| Cadastro de Tratamento e Prévia Matemática de Doses | Validado via `CalculateDoseScheduleUseCaseTest` + `AddTreatmentViewModelTest` + `DoseCertaCujTest` |
| Detalhes do Tratamento, Controle Tomada/Pendente e Exclusão | Validado via `TreatmentDetailViewModelTest` + `ToggleDoseStatusUseCaseTest` |
| Agendamento e Cancelamento no `AlarmManager` | Validado via `AndroidDoseAlarmSchedulerTest` |
| Consulta OpenFDA + Fallback e Histórico em Cache Offline | Validado via `DrugInfoRepositoryImplTest` + `DrugSearchViewModelTest` + `DoseCertaCujTest` |
| Capturas de tela em dispositivo físico / emulador | Pronto para captura local e anexo em `docs/evidencias/` |

---

## 10. Limitações Conhecidas

- **Idioma da Base OpenFDA:** A OpenFDA Drug Labeling API disponibiliza bulas registradas nos Estados Unidos majoritariamente em **inglês** (princípios ativos como *ibuprofen*, *amoxicillin*, *acetaminophen*), podendo não retornar nomes comerciais exclusivos do mercado brasileiro.
- **Restrições de Economia de Bateria (OEMs):** Em alguns fabricantes Android com otimização agressiva de bateria, alarmes exatos podem sofrer pequenas variações caso o usuário restrinja execução em segundo plano ou revogue a permissão `SCHEDULE_EXACT_ALARM`.
- **Caráter Informativo:** As advertências e posologias exibidas servem apenas como apoio informativo e não substituem prescrição ou acompanhamento médico.
- **Escopo Local (v1.0):** Não há criação de conta ou sincronização em nuvem entre múltiplos dispositivos nesta versão, priorizando privacidade e operação 100% Offline-First.
