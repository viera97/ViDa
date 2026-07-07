# ViDa

App Android para gestionar finanzas personales en Cuba.

## Stack

- Kotlin 2.3.21 + JVM 17
- Jetpack Compose + Material 3
- Clean Architecture (domain / data / feature / core) + MVVM
- Hilt (DI), Room (DB — pendiente), DataStore (preferencias — pendiente)
- KSP, Coroutines + Flow, Navigation Compose (pendiente)

## Módulos

| Módulo          | Tipo                | Rol                                                              |
| --------------- | ------------------- | ---------------------------------------------------------------- |
| `:app`          | Android Application | Entry point, Activity, tema, navegación                          |
| `:core`         | Android Library     | Utilidades comunes, theme bridge, extensiones                    |
| `:domain`       | Kotlin/JVM          | Entidades puras, casos de uso, contratos de repositorio          |
| `:data`         | Android Library     | Room (pendiente), DataStore (pendiente), implementación de repos |
| `:feature-home` | Android Library     | Pantalla de inicio (placeholder en PR #2)                        |

## Cómo construir

```bash
./gradlew :app:assembleDebug
```
