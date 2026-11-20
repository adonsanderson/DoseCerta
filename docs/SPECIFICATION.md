# Especificação de Software (SDD) — DoseCerta

> **Projeto:** DoseCerta (`dose-certa-android`)  
> **Pacote Base:** `com.example.dosecerta`  
> **Plataforma:** Android Nativo (Kotlin + Jetpack Compose)  
> **Paradigma:** Offline-First + Clean Architecture  
> **Versão do Documento:** 1.0 (Marco 1 — Fundação e Documentação SDD)

---

## 1. Visão Geral da Aplicação

O **DoseCerta** é um aplicativo Android nativo, projetado sob a arquitetura **Offline-First**, destinado ao gerenciamento de tratamentos medicamentosos, cálculo automático de cronogramas de administração, disparo confiável de lembretes sonoros locais e consulta de informações farmacológicas (bulas e advertências).

### 1.1 Problema Resolvido

A baixa adesão a tratamentos medicamentosos é um dos principais fatores de falha terapêutica em tratamentos contínuos e de curto prazo (como antibióticos, anti-inflamatórios e analgésicos). No cotidiano, pacientes e cuidadores enfrentam três dores centrais:

1. **Erro no cálculo manual de horários:** Calcular manualmente doses em intervalos de 6h, 8h ou 12h ao longo de vários dias — atravessando madrugadas, viradas de dia ou de mês — leva a atrasos, esquecimentos ou superdosagens.
2. **Dependência de conectividade para lembretes:** Aplicativos dependentes de nuvem ou *push notifications* falham quando o dispositivo está sem sinal de internet, em modo avião ou em regiões com cobertura instável.
3. **Dificuldade de acesso rápido a advertências e bulas:** Pacientes frequentemente descartam bulas físicas e precisam consultar indicações, contraindicações ou efeitos adversos rapidamente, inclusive quando estão offline.

### 1.2 Objetivo do Sistema

Fornecer uma experiência simples, confiável e resiliente que:
- Calcule automaticamente **todos os horários exatos** de um tratamento a partir do horário inicial, intervalo em horas e duração em dias;
- Dispare **alarmes locais exatos com notificação sonora** (`AlarmManager` + `BroadcastReceiver`), mesmo com o aplicativo fechado, tela bloqueada ou sem conexão à internet;
- Permita acompanhar a **adesão medicamentosa** dose a dose ("Tomada" vs. "Pendente");
- Permita consultar **informações farmacológicas e advertências** via API REST pública (**OpenFDA**) e armazenar o resultado localmente para consultas futuras sem internet.

---

## 2. Justificativa Técnica da API REST (OpenFDA)

Para o módulo de consulta de informações farmacológicas e advertências (RF05), o projeto adota a **OpenFDA Drug Labeling API**:

- **Endpoint Base:** `https://api.fda.gov/`
- **Recurso Utilizado:** `GET /drug/label.json`
- **Exemplo de Consulta:** `https://api.fda.gov/drug/label.json?search=openfda.brand_name:"ibuprofen"+openfda.generic_name:"ibuprofen"&limit=10`
- **Documentação Oficial:** https://open.fda.gov/apis/drug/label/

### 2.1 Por que a OpenFDA Drug Labeling API?

| Critério | Justificativa Técnica |
| :--- | :--- |
| **Acesso Público e Gratuito** | Mantida pela *U.S. Food and Drug Administration (FDA)* como iniciativa de dados abertos governamentais, sem custo de licenciamento ou assinatura. |
| **Sem Necessidade de API Key** | Permite requisições diretas sem chave de API (até 240 requisições/minuto e 1.000 requisições/dia por IP), simplificando a configuração local, eliminando exposição de segredos no repositório e viabilizando execução imediata por qualquer avaliador. |
| **Riqueza Clínica Estruturada** | Retorna JSON padronizado contendo nome comercial (`openfda.brand_name`), princípio ativo (`openfda.generic_name`), fabricante (`openfda.manufacturer_name`), indicações (`indications_and_usage`), advertências (`warnings` / `warnings_and_cautions`), posologia (`dosage_and_administration`), contraindicações (`contraindications`) e reações adversas (`adverse_reactions`). |
| **Sinergia com Offline-First** | Como dados de bula mudam com baixa frequência, cada consulta bem-sucedida é persistida no banco local **Room**, reduzindo chamadas repetidas à API (evitando *rate limit*) e permitindo leitura instantânea sem internet. |

### 2.2 Aviso de Responsabilidade Clínica

Todas as telas que exibem informações obtidas da OpenFDA devem apresentar um aviso explícito informando que os dados são de caráter estritamente informativo, originados da base pública da FDA, e **não substituem orientação médica ou farmacêutica profissional**.

---

## 3. Requisitos Funcionais (RF01 – RF07)

### RF01 — Cadastro de Tratamento
- **Descrição:** O usuário deve poder cadastrar um novo tratamento medicamentoso fornecendo os parâmetros essenciais para geração do cronograma.
- **Entradas Obrigatórias:**
  - Nome do medicamento (texto não vazio, ex.: `"Amoxicilina 500mg"`).
  - Data e hora da primeira dose (selecionáveis via seletores nativos de data e hora).
  - Intervalo entre doses em horas (valores rápidos sugeridos como `4h`, `6h`, `8h`, `12h`, `24h` ou valor inteiro customizado entre `1` e `168` horas).
  - Duração do tratamento em dias (valor inteiro positivo entre `1` e `365` dias).
- **Entradas Opcionais:**
  - Dosagem/concentração (ex.: `"1 comprimido"`, `"5 ml"`).
  - Observações adicionais (ex.: `"Tomar após as refeições"`).
- **Regras de Validação:**
  - Impedir submissão com nome vazio ou em branco.
  - Rejeitar intervalo `<= 0` ou duração `<= 0`.
  - Exibir feedback inline nos campos inválidos antes de persistir.
- **Critério de Aceitação:** Ao salvar um tratamento válido, o sistema persiste o tratamento, gera todas as doses correspondentes (RF02), agenda os alarmes das doses futuras (RF03) e retorna à listagem atualizada.

### RF02 — Cálculo Automático de Doses
- **Descrição:** A partir dos dados de um tratamento (timestamp da primeira dose, intervalo em horas e duração em dias), o sistema deve calcular automaticamente a sequência completa de horários exatos de administração.
- **Regra de Cálculo:**
  - A primeira dose ocorre em `startTimeMillis` (`t_0`).
  - As doses subsequentes ocorrem em `t_n = startTimeMillis + (n * intervalHours * 3_600_000L)`.
  - O horizonte total do tratamento é `endTimeMillis = startTimeMillis + (durationDays * 24L * 3_600_000L)`.
  - São geradas todas as doses tais que `t_n < endTimeMillis` (ex.: tratamento de `8h` em `8h` por `1 dia` gera 3 doses: `08:00`, `16:00`, `00:00`; `12h` em `12h` por `5 dias` gera 10 doses).
  - O algoritmo deve lidar corretamente com transições de dia, fim de mês, anos bissextos e cálculo determinístico baseado em epoch em milissegundos na camada de domínio pura.
- **Critério de Aceitação:** A lista de doses geradas está estritamente ordenada cronologicamente, numerada sequencialmente (`1..N`) e inicializada com status `PENDING`.

### RF03 — Despertador e Notificações Locais
- **Descrição:** Para cada dose futura agendada, o aplicativo deve programar um alarme local exato utilizando `AlarmManager` e `BroadcastReceiver`, disparando uma notificação sonora com o nome do medicamento, dosagem e horário programado.
- **Comportamento Esperado:**
  - Funcionar com o aplicativo em segundo plano, encerrado pelo usuário (*killed*) ou com o aparelho sem acesso à internet.
  - Utilizar canal de notificação de alta importância (`IMPORTANCE_HIGH`) com som de alarme/notificação padrão do sistema e vibração.
  - Tratar permissões modernas do Android:
    - `POST_NOTIFICATIONS` (Android 13+ / API 33+): solicitação em tempo de execução com explicação clara.
    - `SCHEDULE_EXACT_ALARM` (Android 12+ / API 31+): verificação via `AlarmManager.canScheduleExactAlarms()` com fallback seguro ou direcionamento às configurações do sistema quando necessário.
  - Reagendar automaticamente alarmes pendentes após reinicialização do dispositivo via `BroadcastReceiver` escutando `ACTION_BOOT_COMPLETED`.
  - Permitir cancelar alarmes individuais (quando a dose é marcada como tomada antecipadamente) ou todos os alarmes de um tratamento (quando o tratamento é excluído).
  - Disponibilizar ação rápida direta na notificação para marcar a dose como **"Tomada"**.
- **Critério de Aceitação:** No horário exato da dose, a notificação sonora é exibida contendo o nome do medicamento e o horário, sem depender de rede ou de o app estar aberto.

### RF04 — Controle de Adesão
- **Descrição:** O usuário deve poder alternar o status de cada dose agendada entre **"Tomada"** (`TAKEN`) e **"Pendente"** (`PENDING`).
- **Comportamento Esperado:**
  - Ao marcar uma dose como `TAKEN`, registrar o timestamp efetivo da confirmação (`takenAtMillis`) e cancelar eventual alarme ainda pendente para aquela dose.
  - Ao reverter uma dose para `PENDING`, limpar `takenAtMillis` e, se o horário programado ainda estiver no futuro, reagendar o alarme correspondente.
  - Exibir na interface a taxa/progresso de adesão do tratamento (ex.: `"6 de 15 doses tomadas — 40%"`) e destaque visual diferenciado para doses pendentes, atrasadas e tomadas.
- **Critério de Aceitação:** Alterações de status são persistidas imediatamente no Room e refletidas em tempo real em todas as telas observadoras via `StateFlow`.

### RF05 — Consulta de Medicamentos via API REST + Cache Offline
- **Descrição:** O usuário deve poder pesquisar medicamentos pelo nome (comercial ou princípio ativo) utilizando a API OpenFDA via Retrofit e consultar advertências, indicações, posologia e contraindicações.
- **Estratégia de Cache Offline:**
  - Antes ou em paralelo à requisição remota, o histórico de medicamentos previamente consultados fica disponível localmente no Room.
  - Ao receber uma resposta bem-sucedida da OpenFDA, os resultados relevantes são normalizados e salvos/atualizados no banco Room com timestamp da consulta (`cachedAtMillis`).
  - Se o dispositivo estiver offline ou ocorrer erro de rede ao buscar um termo já consultado anteriormente, o sistema exibe os registros armazenados em cache com indicação visual de que os dados estão sendo exibidos em modo offline.
  - A tela exibe também a lista de **Consultas Recentes (Cache Offline)** para acesso imediato sem digitar novamente ou sem internet.
- **Critério de Aceitação:** Uma bula consultada com internet permanece completamente acessível após ativar o Modo Avião e reiniciar o aplicativo.

### RF06 — Persistência Local Offline-First
- **Descrição:** Todos os tratamentos cadastrados, cronogramas de doses, registros de adesão e bulas consultadas devem ser armazenados localmente em banco SQLite gerenciado pelo **Room**.
- **Single Source of Truth (SSOT):**
  - A UI observa exclusivamente fluxos reativos (`Flow`) provenientes do banco Room.
  - Operações de escrita (criação de tratamento, marcação de dose, exclusão, cache de bula) atualizam o Room, que propaga automaticamente o novo estado às camadas superiores.
  - Integridade referencial garantida via chave estrangeira (`ForeignKey` com `ON DELETE CASCADE`) entre `treatments` e `doses`.
- **Critério de Aceitação:** Nenhum dado do usuário é perdido ao fechar o app ou reiniciar o dispositivo; todas as funcionalidades principais (RF01 a RF04 e leitura de cache do RF05) funcionam 100% offline.

### RF07 — Gerenciamento Visual de Estados
- **Descrição:** Todas as telas que realizam operações assíncronas (leitura de banco, salvamento, cálculo ou consulta de rede) devem modelar e renderizar explicitamente quatro estados visuais distintos:
  1. **`Loading`:** Indicador de carregamento claro (ex.: `CircularProgressIndicator` ou estado de progresso contextual).
  2. **`Success`:** Renderização completa dos dados com interações habilitadas.
  3. **`Empty`:** Estado vazio ilustrativo e orientativo com *Call-to-Action* (ex.: "Nenhum tratamento cadastrado" com botão para adicionar o primeiro; "Nenhuma bula encontrada para este termo").
  4. **`Error`:** Mensagem descritiva e amigável (sem jargões técnicos ou stack traces) acompanhada de ação de recuperação (ex.: botão **"Tentar novamente"** ou acesso aos dados em cache).
- **Critério de Aceitação:** Nenhuma tela apresenta tela em branco silenciosa ou travamento sem feedback durante carregamento, ausência de resultados ou falha de rede.

---

## 4. Requisitos Não Funcionais (RNF01 – RNF05)

### RNF01 — Clean Architecture e Pureza da Camada de Domínio
- O projeto deve ser dividido rigorosamente em três camadas principais (`Presentation`, `Domain`, `Data`), acrescidas do módulo de infraestrutura de alarmes (`alarm/`).
- A camada **`Domain`** deve ser **100% Kotlin puro**:
  - **Proibido** importar qualquer pacote `android.*`, `androidx.*`, `retrofit2.*`, `okhttp3.*` ou anotações do `Room` no pacote `domain`.
  - O algoritmo de cálculo de doses (`CalculateDoseScheduleUseCase`) e todas as regras de negócio devem executar diretamente em JVM pura.

### RNF02 — Assincronismo e Reatividade com Coroutines e StateFlow
- Todas as operações de I/O (acesso ao Room e requisições HTTP OpenFDA) devem executar fora da Main Thread usando **Kotlin Coroutines** (`suspend`, `Flow`, `Dispatchers.IO`).
- O estado das telas deve ser exposto exclusivamente como `StateFlow` imutável (`asStateFlow()`) pelos ViewModels e coletado na UI de forma segura ao ciclo de vida via `collectAsStateWithLifecycle()`.
- É proibido o uso de `GlobalScope` ou bloqueio da thread principal (`runBlocking` em código de produção).

### RNF03 — Interface Moderna com Jetpack Compose, Performance e Navigation Compose
- 100% da interface deve ser construída em **Jetpack Compose** seguindo as diretrizes do **Material Design 3** (incluindo suporte a tema claro/escuro e `enableEdgeToEdge()`).
- **Otimização de Recomposição:**
  - Uso de modelos de UI imutáveis (`@Immutable` / `@Stable`) e coleções estáveis.
  - Definição obrigatória de `key` estável em todos os `LazyColumn` / `LazyRow`.
  - Uso de `remember`, `derivedStateOf` e lambdas estáveis para evitar recomposições desnecessárias em listas de doses.
- **Navegação:** Roteamento estruturado com **Navigation Compose** e argumentos bem tipados entre as telas do aplicativo.
- **Acessibilidade:** Touch targets mínimos de `48.dp` e `contentDescription` significativos em ícones e ações.

### RNF04 — Injeção de Dependências com Koin
- Toda a grafo de dependências (Database, DAOs, Retrofit/OkHttp, Repositórios, UseCases, `DoseAlarmScheduler` e ViewModels) deve ser gerenciada pelo **Koin** (`koin-android`, `koin-androidx-compose`).
- As definições devem ser segmentadas em módulos coesos (`domainModule`, `dataModule`, `networkModule`, `alarmModule`, `presentationModule`), inicializados na classe `Application`.

### RNF05 — Qualidade, Testabilidade e Testes Unitários
- O projeto deve possuir suíte automatizada de **testes unitários locais (JVM)** utilizando:
  - **JUnit 4** para estruturação e asserções de testes;
  - **MockK** para criação de dublês de teste (mocks/stubs/spies) de repositórios, DAOs, APIs e agendadores;
  - **Turbine** (`app.cash.turbine`) e **`kotlinx-coroutines-test`** (`runTest`, `StandardTestDispatcher`) para validação determinística de `Flow` e transições de `StateFlow`.
- Cobertura obrigatória mínima de testes unitários:
  1. Algoritmo de cálculo de cronograma de doses (`CalculateDoseScheduleUseCase`), incluindo casos de borda (intervalos diversos, virada de dia/mês/ano, entradas inválidas);
  2. Repositórios (`TreatmentRepositoryImpl` e `DrugInfoRepositoryImpl`), validando estratégia Offline-First, cache e tratamento de exceções;
  3. ViewModels (`HomeViewModel`, `AddTreatmentViewModel`, `DrugSearchViewModel`), validando transições entre `Loading`, `Success`, `Empty` e `Error`.

---

## 5. Regras de Negócio Consolidadas

| Código | Regra de Negócio |
| :--- | :--- |
| **RN01** | Todo tratamento deve possuir ao menos 1 dose gerada; caso `intervalHours` seja maior que `durationDays * 24`, ainda assim a primeira dose (`t_0`) é registrada. |
| **RN02** | A exclusão de um tratamento deve remover em cascata todas as suas doses do banco local e cancelar todos os alarmes pendentes no `AlarmManager`. |
| **RN03** | Doses cujo horário agendado já passou e ainda estão com status `PENDING` são classificadas visualmente como **"Atrasadas"** para priorização do usuário. |
| **RN04** | Alarmes só são agendados no `AlarmManager` para doses com status `PENDING` e `scheduledTimeMillis > currentTimeMillis`. |
| **RN05** | O identificador único (`requestCode`) do `PendingIntent` de cada alarme é derivado deterministicamente do `doseId` persistido no Room, evitando colisões entre medicamentos diferentes. |
| **RN06** | Consultas à OpenFDA com termo vazio ou menor que 2 caracteres não disparam requisição HTTP, exibindo diretamente o histórico em cache offline. |

---

## 6. Escopo do Projeto

### In-Scope (Previsto na v1.0)
- Cadastro, visualização e exclusão de tratamentos medicamentosos.
- Cálculo automático completo de cronograma de doses.
- Disparo de alarmes exatos locais e notificações sonoras offline + reagendamento pós-boot.
- Controle de adesão por dose (Tomada / Pendente) com barra de progresso.
- Consulta de bulas/advertências na API OpenFDA com persistência em cache offline.
- Suíte de testes unitários para Domain, Data e Presentation.

### Out-of-Scope (Fora do escopo da v1.0)
- Autenticação de usuários e sincronização em nuvem (foco total em privacidade e operação Offline-First).
- Leitura de código de barras ou receitas via câmera/OCR.
- Interação medicamentosa automatizada com IA ou prescrição clínica.
- Integração com wearables (Wear OS).
