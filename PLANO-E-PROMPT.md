# GGLauncher — Plano Completo + Prompt Ultra-Detalhado

> Stack escolhida (a melhor p/ launcher leve): **Kotlin + Views XML (View System)**.
> Motivo: launcher fica 100% do tempo em memória — Views alocam menos, cold start ~2x mais rápido que Compose, APK menor, scrolling da drawer a 120Hz sem jank. Compose seria mais bonito de codar, mas mais pesado em runtime. Decisão técnica, não gosto pessoal.

---

## 1. Visão do produto

**GGLauncher** = launcher nativa Android, OneUI-like mas mais leve que a One UI original. Público: celular fraco/intermediário que quer cara de Galaxy sem o peso da Samsung. Meta: **APK < 8 MB, cold start < 400ms, RAM idle < 80 MB, 60/120fps na drawer**.

## 2. Escopo fechado (v1.0)

| Módulo | Inclui | Fora do v1 |
|---|---|---|
| Home | grade 4x5/5x5/5x6, páginas múltiplas, dock 4-5 ícones, pastas com preview, wallpaper parallax leve | transição 3D, feeds |
| App Drawer | vertical paginado estilo OneUI (busca embaixo, letras rápidas na lateral), categorias auto (Sugeridos/Todos) | drawer horizontal |
| Busca universal | apps + contatos (opcional) + atalho Play Store, T9 opcional | busca web |
| Widgets | host de widgets do sistema (AppWidgetHost) + **relógio GG editável próprio** | widgets próprios de clima etc |
| Temas | 3 temas: **AMOLED Black** (#000000 puro), **Dark Normal** (#121212/Material dark), **Light** (#F5F5F5/OneUI light) + Material You dinâmico (Android 12+) opcional | loja de temas |
| Ícones | pack de ícones adaptativos próprio (círculo/squircle OneUI), suporte a packs externos (Nova/Apex API) | icon maker |
| Gestos | swipe up = drawer, swipe down = notificações, double-tap = bloquear tela (DeviceAdmin), pinch = overview | gestos por app |
| Config | tela de settings OneUI-like (SwitchPreference, SeekBar tamanho grade/ícone, backup/restore JSON do layout) | cloud sync |
| Extras | esconder apps (com PIN), badge de notificação (NotificationListener), atalhos long-press (App Shortcuts), modo gamer (limpa RAM + DND) | — |

## 3. Arquitetura (MVVM + Clean-lite)

```
app/
├── manifest (MAIN/HOME/DEFAULT/LAUNCHER category, AppWidgetHost, BIND_NOTIFICATION_LISTENER, BIND_APPWIDGET)
├── ui/
│   ├── home/        (LauncherActivity, WorkspaceView, CellLayout, PageIndicator, DockView, FolderView)
│   ├── drawer/      (DrawerFragment/BottomSheet, AppListAdapter c/ DiffUtil + filtro, FastScroller, SearchBar)
│   ├── widgets/    (WidgetHostFragment, GGClockWidgetProvider + ClockConfigActivity)
│   ├── settings/   (SettingsActivity c/ PreferenceFragmentCompat, ThemeManager, BackupManager)
│   └── gestures/   (GestureHandler, DoubleTapDetector, ScreenLockAdmin)
├── data/
│   ├── apps/       (AppRepository: PackageManager cache + BroadcastReceiver INSTALL/REMOVE/CHANGE, AppModel)
│   ├── layout/     (WorkspaceStore: Room ou Proto DataStore — posição de cada ícone/pasta/página)
│   └── prefs/      (DataStore: tema, grade, tamanho ícone, dock count, apps ocultos, clock style)
├── theming/        (ThemeManager: 3 temas + dynamic color, attrs customizados, night modes)
├── icons/          (AdaptiveIconHelper, IconPackParser: parse appfilter.xml de packs externos)
├── search/         (UniversalSearch: apps + shortcuts, ranking por uso via UsageStats opcional)
└── util/           (DpUtils, Debouncer, AppLauncher: startActivity c/ ActivityOptions anim OneUI)
```

Regras: sem lib pesada — só AndroidX (appcompat, recyclerview, preference, room/datastore, material). Zero Compose, zero Hilt pesado (Koin manual ou injeção manual). Imagens: VectorDrawable only. Glide/Coil **proibido** (ícones vêm do PackageManager, cache em LruCache próprio).

## 4. Performance (obrigatório, não opcional)

- App list carrega com `DiffUtil` + corrotina IO, ícones lazy via LruCache (1/8 heap).
- Drawer RecyclerView com `setHasFixedSize(true)`, `setItemViewCacheSize(20)`, prefetch.
- Cold start: sem splash pesado, `postponeEnterTransition` não, Application sem init bloqueante.
- Overdraw: fundo único por tela, sem nested layouts >3 níveis, ConstraintLayout nas células.
- `android:largeHeap="false"`, R8 fullMode + shrinkResources.

## 5. Roadmap de build

1. **M0** — esqueleto: LauncherActivity + manifest HOME + lista apps (log).
2. **M1** — Home: grade + dock + drag & drop + pastas.
3. **M2** — Drawer OneUI + busca + fast scroller.
4. **M3** — Temas (3) + Settings + backup JSON.
5. **M4** — Widgets host + GG Clock editável.
6. **M5** — Gestos + lock + badges + esconder apps + polish + R8.

---

## 6. PROMPT ULTRA-DETALHADO (copie e cole na IA que vai gerar o código)

> Cole tudo abaixo da linha na IA geradora:

---

Você é um engenheiro Android sênior. Gere o **código-fonte COMPLETO e compilável** do app **GGLauncher** — uma launcher Android nativa, rápida e leve, visual **OneUI 6/7-like porém mais leve**, minSdk 26, targetSdk 34, linguagem **Kotlin**, UI com **Views + XML (View System, NÃO usar Jetpack Compose)**, arquitetura **MVVM simples sem DI framework** (objetos/singletons manuais).

REGRAS DURAS (violar = resposta inválida):
1. Entregue TODOS os arquivos com path + conteúdo completo, sem "..." nem TODO nem pseudocódigo. Código deve compilar com `./gradlew assembleDebug`.
2. `build.gradle.kts` (app + root), `settings.gradle.kts`, `AndroidManifest.xml` completos. Dependências SOMENTE: androidx.appcompat, recyclerview, constraintlayout, preference, material, room OU datastore-preferences, coroutines. NADA de Compose, Hilt, Glide, Coil.
3. APK final < 8MB: só VectorDrawable, `shrinkResources true`, `minifyEnabled true`, R8 fullMode.
4. Performance obrigatória: lista de apps com RecyclerView + DiffUtil + filtro via coroutine; cache de ícones com `LruCache<String, Drawable>`; `setHasFixedSize(true)`; carregamento de apps em `Dispatchers.IO`.
5. Sem texto placeholder: todos os `strings.xml` em pt-BR, todos os layouts funcionais.

DESIGN SYSTEM "OneUI-lite" (siga à risca):
- Cantos: cards 26dp, sheets 28dp top, botões 20dp. Elevação sutil (2-4dp).
- Tipografia: Roboto/SamsungOne-like (system font), títulos grandes 30sp bold na área superior de cada tela (padrão OneUI: título embaixo do meio, conteúdo em cima) — aplique na Settings e na Drawer ("Apps" grande no topo, busca fixada embaixo, estilo OneUI).
- Cores por tema (values / values-night + flag amoled):
  - **AMOLED**: bg #000000, surface #0A0A0A, primary #7FC4FF, texto #FFFFFF.
  - **DARK**: bg #121212, surface #1E1E1E, primary #4FA3FF, texto #FFFFFF.
  - **LIGHT**: bg #F5F5F5, surface #FFFFFF, primary #0367C2, texto #1A1A1A.
  - Implemente `ThemeManager` com DataStore (`theme_mode`: amoled/dark/light/system + `dynamic_color`: bool p/ Android 12+).
- Ícones adaptativos squircle (máscara OneUI) gerados via `AdaptiveIconDrawable` + fundo branco/colorido por app.

TELAS E ARQUIVOS OBRIGATÓRIOS (gere todos):
A) **Manifest + LauncherActivity** (`ui.home.LauncherActivity`): category HOME/DEFAULT, Workspace com ViewPager2 de páginas (CellLayout 5x5 configurável 4x5/5x5/5x6 via settings), Dock (4-5 slots), PageIndicator estilo OneUI (pílula). Drag&drop entre células (View.OnDragListener), criar pasta ao soltar ícone sobre ícone (FolderView com preview 3x3 mini + nome editável).
B) **App Drawer** (`ui.drawer.DrawerBottomSheet` estendendo BottomSheetDialogFragment, peek 60%, expandido full): RecyclerView vertical 4 colunas + SearchBar fixa INFERIOR (OneUI) + FastScroller lateral com letras + header "Apps" grande. Adapter com DiffUtil, SectionIndexer simples. Toque abre app com animação `ActivityOptions.makeScaleUpAnimation`. Long-press mostra popup (Info do app, Desinstalar, Adicionar à home, Ocultar).
C) **Busca universal**: filtra apps por nome (contains, ignore case/acentos via Normalizer) com debounce 150ms; T9 opcional (2=ABC...) como setting bool.
D) **Widgets**: `WidgetHostFragment` com AppWidgetHost real (pick/bind via AppWidgetManager, requestBindWidget), lista de widgets do sistema; **GGClockWidget próprio** (`widgets.GGClockWidgetProvider` + `ClockConfigActivity`): relógio GRANDE editável estilo OneUI — settings de: formato 12/24h, mostrar/ocultar data, 6 fontes (system serif/sans/mono + 3 custom via font res), 8 cores, tamanho (S/M/L/XL), estilo (digital clean / digital bold / flip / outline), fundo (transparente/blur/cardia). Preview ao vivo na config. Atualiza via WorkManager ou tick a cada minuto (ACTION_TIME_TICK receiver).
E) **Settings** (`ui.settings.SettingsActivity` + prefs XML): aparência (tema radio: Amoled/Dark/Light/Sistema + dynamic color switch), grade da home (ListPreference 4x5/5x5/5x6), tamanho ícones (SeekBar 40-72dp), dock count (4/5), drawer colunas (4/5), T9, duplo-toque-ação, backup/restore JSON do layout (botões export/import via SAF), sobre.
F) **Gestos** (`util.GestureHandler`): swipe-up abre drawer, swipe-down abre painel notificações (expand via AccessibilityService? use `STATUS_BAR_SERVICE` reflection fallback — ou simplesmente `performGlobalAction` NÃO; implemente via intent de quick settings + documente limitação; duplo-tap bloqueia via DeviceAdminReceiver `ScreenLockAdmin`), pinch abre overview (Activity de preview das páginas).
G) **Dados**: `AppRepository` (queryIntentActivities MAIN/LAUNCHER, cache em memória, BroadcastReceiver PACKAGE_ADDED/REMOVED/CHANGED atualiza lista), `WorkspaceStore` (Room: entities WorkspaceItem(id, packageName, componentName, page, cellX, cellY, type: APP/FOLDER/WIDGET, folderId, widgetId, spanX, spanY) + DAO + migrations), prefs via DataStore.
H) **Esconder apps** com PIN (SHA-256 do PIN em DataStore, tela de unlock), **badges** via NotificationListenerService (contagem por pacote, dot OneUI no ícone), **App Shortcuts** long-press (ShortcutManager), **atalhos desinstalar/info**.
I) **BackupManager**: serializa WorkspaceStore + prefs em JSON, export/import com Storage Access Framework.

QUALIDADE DE ENTREGA:
- Ordene a resposta por: 1) gradle/manifest, 2) data, 3) theming/icons/util, 4) ui.home, 5) ui.drawer+search, 6) widgets+clock, 7) settings+backup, 8) gestures+badges+lock, 9) resources (themes.xml ×3, colors, dimens, strings pt-BR, layouts XML de cada tela, drawable vectors, font res), 10) instruções de build e teste (comandos + checklist manual: definir como launcher padrão, arrastar, pasta, trocar os 3 temas, adicionar widget, relógio editável, backup/restore).
- Cada arquivo: bloco ```caminho: path/Arquivo.ext``` + código completo.
- Se a resposta for longa demais, divida em PARTES numeradas (Parte 1, 2...) sem cortar arquivo no meio, e termine com checklist de compilação.
- Revise mentalmente: imports corretos, R.* consistente com resources gerados, sem referência a arquivo não entregue.

Comece AGORA pela Parte 1.

---

## 7. Como usar

1. Cole o prompt da seção 6 numa IA forte (ou aqui comigo por partes).
2. Sugestão de ordem se for gerar comigo: M0 → M1 → M2 → M3 → M4 → M5 (fala "gera M1" que eu gero).
3. Depois: `cd GGlauncher && ./gradlew assembleDebug`, instala o APK, define como padrão e testa o checklist.
