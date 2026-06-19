# ViDa

App Android para gestionar finanzas personales en Cuba. Estado: **scaffold inicial** (PR #1 de 2).

## Stack

- Kotlin 2.3.21 + JVM 17
- Jetpack Compose + Material 3
- Clean Architecture (domain / data / feature / core) + MVVM
- Hilt (DI), Room (DB — pendiente), DataStore (preferencias — pendiente)
- KSP, Coroutines + Flow, Navigation Compose (pendiente)

## Módulos

| Módulo | Tipo | Rol |
| --- | --- | --- |
| `:app` | Android Application | Entry point, Activity, tema, navegación |
| `:core` | Android Library | Utilidades comunes, theme bridge, extensiones |
| `:domain` | Kotlin/JVM | Entidades puras, casos de uso, contratos de repositorio |
| `:data` | Android Library | Room (pendiente), DataStore (pendiente), implementación de repos |
| `:feature-home` | Android Library | Pantalla de inicio (placeholder en PR #2) |

## Cómo construir

```bash
./gradlew :app:assembleDebug
```

> **Nota**: en este punto (PR #1) el comando falla por diseño: falta `ViDaApplication` y `MainActivity`. Eso llega en PR #2.

## Cómo correr

Abrir el proyecto en Android Studio (Ladybug o superior, con AGP 9.2.0) y correr la app en un emulador o dispositivo API 26+.

## Estado

- ✅ PR #1: Gradle scaffold + module skeleton (esta entrega)
- ⏳ PR #2: Compose theme + Hola ViDa shell
- ⏳ PR #3+: Features (tarjetas, baquitas, gastos, transferencias, tasas)
