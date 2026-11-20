# DoseCerta

Aplicativo Android nativo **Offline-First** para gerenciamento de tratamentos medicamentosos, cálculo automático de cronograma de doses, despertador local com notificações sonoras (`AlarmManager` + `BroadcastReceiver`) e consulta de bulas/advertências farmacológicas via API REST pública **OpenFDA** com cache offline.

> **Repositório:** `dose-certa-android`  
> **Pacote:** `com.example.dosecerta`  
> **Status do Projeto:** Marco 1 — Fundação e Documentação SDD concluído

---

## 1. Objetivo

Aumentar a segurança e a adesão a tratamentos medicamentosos eliminando erros no cálculo manual de horários, garantindo que os lembretes sonoros sejam disparados pontualmente **mesmo sem acesso à internet ou com o aplicativo fechado**, e disponibilizando consulta rápida a advertências e informações farmacológicas com persistência local para leitura offline.

---

## 2. Funcionalidades Planejadas

- **Cadastro de Tratamentos (RF01):** Registro de medicamento, dosagem/observações, data/hora da primeira dose, intervalo em horas (ex.: 4h, 6h, 8h, 12h, 24h) e duração em dias.
- **Cálculo Automático de Cronograma (RF02):** Geração determinística de todos os horários exatos de administração ao longo de todo o período do tratamento, respeitando viradas de dia, mês e ano.
- **Despertador e Notificações Locais (RF03):** Agendamento de alarmes exatos via `AlarmManager` e `BroadcastReceiver` com notificação sonora de alta prioridade, ação rápida para marcar como tomada e reagendamento automático pós-reinicialização do dispositivo (`BOOT_COMPLETED`).
- **Controle de Adesão (RF04):** Marcação de cada dose como **"Tomada"** ou **"Pendente"**, destaque de doses atrasadas e acompanhamento visual do progresso do tratamento.
- **Consulta de Bulas via OpenFDA + Cache Offline (RF05):** Busca de informações e advertências farmacológicas via API REST pública com gravação automática no Room para consulta futura offline.
- **Persistência Offline-First (RF06):** Banco SQLite via Room como *Single Source of Truth* para tratamentos, doses e histórico de bulas.
- **Estados Visuais Explícitos (RF07):** Tratamento completo dos estados `Loading`, `Success`, `Empty` e `Error` em toda a interface.

---

## 3. Tecnologias e Bibliotecas

| Categoria | Tecnologia / Biblioteca |
| :--- | :--- |
| **Linguagem** | Kotlin |
| **Interface (UI)** | Jetpack Compose + Material Design 3 |
| **Navegação** | Navigation Compose |
| **Concorrência & Estado** | Kotlin Coroutines, `Flow` e `StateFlow` |
| **Persistência Local** | Room (SQLite) + KSP |
| **Rede (API REST)** | Retrofit + OkHttp + Moshi / Kotlinx Serialization |
| **Injeção de Dependências** | Koin (`koin-android`, `koin-androidx-compose`) |
| **Alarmes & Notificações** | Android `AlarmManager`, `BroadcastReceiver`, `NotificationManagerCompat` |
| **Testes Unitários** | JUnit 4, MockK, Turbine, `kotlinx-coroutines-test` |

---

## 4. API REST Utilizada (OpenFDA)

O aplicativo consome a **OpenFDA Drug Labeling API**, mantida pela *U.S. Food and Drug Administration (FDA)*:

- **Base URL:** `https://api.fda.gov/`
- **Endpoint:** `GET /drug/label.json`
- **Autenticação:** **Não requer API Key** (pública e gratuita).
- **Dados Utilizados:** Nome comercial (`openfda.brand_name`), princípio ativo (`openfda.generic_name`), fabricante, indicações de uso (`indications_and_usage`), advertências (`warnings`), posologia (`dosage_and_administration`), contraindicações (`contraindications`) e reações adversas (`adverse_reactions`).
- **Uso Offline-First:** Toda consulta bem-sucedida é persistida no banco Room para consulta offline imediata e redução de requisições repetidas.

> **Aviso:** As informações retornadas pela OpenFDA possuem caráter estritamente informativo e não substituem a orientação de um médico ou farmacêutico.

---

## 5. Arquitetura

O **DoseCerta** adota **Clean Architecture** combinada com **MVVM** e **Unidirectional Data Flow (UDF)**:

- **`domain/` (100% Kotlin Puro):** Modelos de domínio, interfaces de repositório/agendador e UseCases — incluindo o algoritmo puro de cálculo de doses (`CalculateDoseScheduleUseCase`), sem nenhuma dependência de `android.*`, Room ou Retrofit.
- **`data/`:** Implementação dos repositórios, banco local Room (Entities, DAOs), cliente Retrofit OpenFDA (DTOs) e mappers.
- **`alarm/`:** Implementação Android do agendamento de alarmes exatos (`AlarmManager`), receptores de disparo/ação rápida/boot (`BroadcastReceiver`) e gerenciador de notificações sonoras.
- **`presentation/`:** Telas Jetpack Compose, Navigation Compose, tema Material 3, ViewModels e `StateFlow` com estados `Loading`, `Success`, `Empty` e `Error`.
- **`di/`:** Módulos de injeção de dependências Koin.

Para documentação técnica completa, consulte:
- [Especificação de Software (SDD)](docs/SPECIFICATION.md)
- [Arquitetura do Sistema](docs/ARCHITECTURE.md)
- [Plano de Tarefas e Marcos](docs/TASKS.md)

---

## 6. Estratégia Offline-First

O aplicativo utiliza o **Room (SQLite)** como **Single Source of Truth (SSOT)**:

1. **Tratamentos, Doses e Alarmes:** Operam **100% offline**. Ao criar um tratamento ou alterar o status de uma dose, a atualização é gravada no Room, os alarmes são sincronizados no `AlarmManager` e a UI reage automaticamente aos `Flow`s do banco.
2. **Consulta de Bulas (OpenFDA):** Ao buscar um medicamento com internet, os dados retornados são armazenados em cache local no Room. Em cenários sem conectividade ou falha de rede, o aplicativo realiza fallback automático para os registros em cache e disponibiliza o histórico completo de medicamentos já consultados para leitura offline.

---

## 7. Instruções de Configuração e Execução

### Pré-requisitos
- **Android Studio** (Ladybug ou superior recomendado)
- **JDK 11+** (ou JDK 17 compatível com AGP do projeto)
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
5. No primeiro uso em dispositivos Android 13+, conceda a permissão de **Notificações** (e, em Android 12+, permita **Alarmes e Lembretes** quando solicitado) para garantir o disparo exato dos despertadores.

---

## 8. Instruções de Testes

A suíte de testes unitários valida o algoritmo de cálculo de doses (`Domain`), repositórios Offline-First (`Data`) e ViewModels (`Presentation`) diretamente na JVM com **JUnit 4**, **MockK** e **Turbine**.

Para executar todos os testes unitários:

```bash
./gradlew testDebugUnitTest
```

> Em ambientes sem wrapper (`gradlew`), utilize diretamente:
> ```bash
> gradle :app:testDebugUnitTest
> ```

Os relatórios HTML gerados ficam disponíveis em:  
`app/build/reports/tests/testDebugUnitTest/index.html`

---

## 9. Evidências

> **Status:** Em desenvolvimento (será preenchida com capturas de tela, GIFs dos fluxos offline/alarmes e relatório de execução dos testes unitários ao final do Marco 6).

| Fluxo / Tela | Status |
| :--- | :--- |
| Home — Cronograma Diário e Adesão | Em desenvolvimento |
| Cadastro de Tratamento e Cálculo de Doses | Em desenvolvimento |
| Detalhes do Tratamento e Controle Tomada/Pendente | Em desenvolvimento |
| Notificação Sonora Local (`AlarmManager`) | Em desenvolvimento |
| Consulta OpenFDA + Leitura em Cache Offline | Em desenvolvimento |
| Estados `Loading`, `Empty` e `Error` | Em desenvolvimento |

---

## 10. Limitações Conhecidas

- **Idioma da Base OpenFDA:** A OpenFDA Drug Labeling API disponibiliza bulas registradas nos Estados Unidos majoritariamente em **inglês** (nomes genéricos como *ibuprofen*, *amoxicillin*, *acetaminophen*), podendo não retornar nomes comerciais exclusivos do mercado brasileiro.
- **Restrições de Economia de Bateria (OEMs):** Em alguns fabricantes Android com otimização agressiva de bateria, alarmes exatos podem sofrer pequenas variações caso o usuário restrinja execução em segundo plano ou revogue a permissão `SCHEDULE_EXACT_ALARM`.
- **Caráter Informativo:** As advertências e posologias exibidas servem apenas como apoio informativo e não substituem prescrição ou acompanhamento médico.
- **Escopo Local (v1.0):** Não há criação de conta ou sincronização em nuvem entre múltiplos dispositivos nesta versão, priorizando privacidade e operação 100% Offline-First.
