# Tasks: card-currency-dynamic

## Review workload forecast

| Module | Files | Est. lines | Notes |
|--------|-------|-----------|-------|
| domain | 1 | ~3 | `Card.kt`: type change + import |
| data | 6 | ~60 | Entity, mapper, repo, migration, AppDatabase, schema JSON |
| feature-card-management | 5 | ~95 | ViewModel, dialog (biggest), UiState, Screen, ListItem |
| app | 1 | ~12 | FuentesScreen: collect + pass + display fix |
| tests | 4 | ~90 | New migration test + 3 test updates |
| **Total** | **17** | **~260** | **Under 400-line budget → single PR** |

No chained PRs needed.

---

## Phase 1: Domain layer

- [x] 1.1 Change `Card.currency` from `Currency` enum to `String` in `domain/src/main/kotlin/com/vida/domain/model/Card.kt`.
  - `currency: Currency` → `currency: String`
  - Remove `import com.vida.domain.model.Currency` (it's in the same package, but if present, remove it)
  - Remove `Currency` from the init/require block if any references exist
  - **Verify**: `./gradlew :domain:compileKotlin` passes

## Phase 2: Data layer

- [x] 2.1 Change `CardEntity.currency` from `Currency` to `String` in `data/src/main/kotlin/com/vida/data/db/entity/CardEntity.kt`.
  - `currency: Currency` → `currency: String`
  - Remove `import com.vida.domain.model.Currency`
  - No `@ColumnInfo` annotation needed — Room infers TEXT affinity from Kotlin String
  - **Verify**: `./gradlew :data:compileDebugKotlin`

- [x] 2.2 Create `data/src/main/kotlin/com/vida/data/db/Migration1314.kt` — no-op migration from v13 to v14.
  - Mirrors `Migration1213.kt` pattern: `val MIGRATION_13_14: Migration = object : Migration(13, 14) { ... }`
  - Body is empty (no SQL) — the column already stores TEXT codes; only the mapped type changed
  - Add KDoc explaining why no-op
  - **Verify**: File compiles, startVersion==13, endVersion==14

- [x] 2.3 Update `data/src/main/kotlin/com/vida/data/db/AppDatabase.kt`.
  - `version = 13` → `version = 14`
  - Add `MIGRATION_13_14` to the `.addMigrations(...)` chain
  - Import `Migration1314.kt`'s symbol if needed (same package, may not need import)
  - **Verify**: File compiles and lists 13 migrations

- [x] 2.4 Update `data/src/main/kotlin/com/vida/data/mapper/CardMapper.kt`.
  - `toDomain`: line 20 `currency = entity.currency` stays (entity.currency is now String, Card.currency is String — no conversion needed)
  - `toEntity`: line 31 `currency = domain.currency` stays (String → String)
  - Remove `import com.vida.domain.model.Currency`
  - **Verify**: `./gradlew :data:compileDebugKotlin` passes, round-trip works

- [x] 2.5 Update `data/src/main/kotlin/com/vida/data/repository/CardRepositoryImpl.kt`.
  - Line 47: `dao.getById(id)?.currency ?: Currency.CUP` — entity.currency is now `String?` (nullable in DB), so fallback becomes `?: "CUP"`
  - Line 47: `val currency = dao.getById(id)?.currency ?: "CUP"` — then resolve to Currency for Money construction: `val currencyEnum = Currency.fromCode(currency) ?: Currency.CUP`
  - Line 52: `Money.fromMinorUnits(entity.totalCupMinor, currencyEnum)` — use resolved enum
  - Line 54: `Money(BigDecimal.ZERO, currencyEnum)` — use resolved enum
  - Remove `import com.vida.domain.model.Currency` import (? actually it's still needed for the resolution and Money calls, but check after the change)
  - **Verify**: `./gradlew :data:compileDebugKotlin`

- [x] 2.6 Regenerate Room schema JSON.
  - Delete `data/schemas/com.vida.data.db.AppDatabase/13.json`
  - Run `./gradlew :app:assembleFreeDebug` or `./gradlew :data:compileDebugKotlin` with KSP to generate `14.json`
  - **Verify**: `data/schemas/com.vida.data.db.AppDatabase/14.json` exists and `CardEntity`'s `currency` column has TEXT affinity (no `TypeConverter` annotation for that column)

## Phase 3: Feature module (card management)

- [x] 3.1 Add `ListCurrencies` injection and `currencyCodes` StateFlow to `feature-card-management/src/main/kotlin/com/vida/feature/cardmanagement/CardListViewModel.kt`.
  - Add `private val listCurrencies: ListCurrencies` to constructor parameters (and import `com.vida.domain.usecase.currency.ListCurrencies`)
  - Add `val currencyCodes: StateFlow<List<String>> = listCurrencies().map { it.map { ci -> ci.code } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())` (mirror `bankNames` pattern on line 79-81)
  - Change `onAdd` signature: `currency: Currency` → `currency: String` (line 147)
  - Change `onEdit` signature: `currency: Currency` → `currency: String` (line 207)
  - In `onAdd` (line 171): `Money(balance, currency)` → resolve: `val currencyEnum = Currency.fromCode(currency) ?: Currency.CUP`, then `Money(balanceBigDecimal, currencyEnum)`
  - In `onEdit` (line 229): same resolution pattern
  - In `toDisplayItem` (line 360): `currency = currency` stays (both are String now)
  - In `toDisplayItemError` (line 385): `currency = currency` stays
  - In `toDisplayItemError` (line 389): `balanceFormatted = "${currency.symbol} —"` → resolve: `val displayCode = Currency.fromCode(currency)?.symbol ?: currency; "${displayCode} —"`
  - **Verify**: `./gradlew :feature-card-management:compileDebugKotlin`

- [x] 3.2 Add `availableCurrencies` parameter to `CardFormDialog` in `feature-card-management/src/main/kotlin/com/vida/feature/cardmanagement/CardFormDialog.kt`.
  - Add parameter: `availableCurrencies: List<String> = emptyList()` (alongside `availableBanks` on line 72)
  - Change `initialCurrency: Currency = Currency.CUP` → `initialCurrency: String = "CUP"` (line 66)
  - Change internal `var currency by remember { mutableStateOf(initialCurrency) }` → now stores `String` (line 81)
  - Change `onSave` signature: `currency: Currency` → `currency: String` (line 74)
  - **Verify**: File compiles after all 3.3 changes

- [x] 3.3 Replace the `Currency.entries` FilterChip row with `ExposedDropdownMenuBox` in `CardFormDialog.kt`.
  - Remove lines 192-201 (the `Text("Moneda")` + `Row` + `Currency.entries.forEach { FilterChip }` block)
  - Add new dropdown block (mirrors bank dropdown at lines 143-171):
    ```
    var currencyDropdownExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = currencyDropdownExpanded,
        onExpandedChange = { currencyDropdownExpanded = it },
    ) {
        OutlinedTextField(
            value = currency,
            onValueChange = {},
            readOnly = true,
            label = { Text("Moneda") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyDropdownExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = currencyDropdownExpanded,
            onDismissRequest = { currencyDropdownExpanded = false },
        ) {
            availableCurrencies.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        currency = code
                        currencyDropdownExpanded = false
                    },
                )
            }
        }
    }
    ```
  - Add orphan detection (see 3.4)
  - Pre-select first item on add: when `!isEdit`, if `currency` is "CUP" (default) and `availableCurrencies.isNotEmpty()`, pre-select `availableCurrencies.first()` — implemented via `remember` `LaunchedEffect` or default init
  - **Verify**: Dialog renders with dropdown, selecting a currency updates state

- [x] 3.4 Add orphan-currency detection in `CardFormDialog.kt`.
  - Compute: `val isOrphanCurrency = isEdit && initialCurrency !in availableCurrencies && availableCurrencies.isNotEmpty()`
  - When `isOrphanCurrency` is true: `currency = ""` (no selection), show warning `Text` below dropdown: "La moneda asignada ya no existe. Seleccione otra." in `MaterialTheme.colorScheme.error`
  - When orphan: Save disabled regardless of other validations. Update `isSaveEnabled` by adding `&& currency.isNotEmpty() && currency in availableCurrencies` condition
  - When user picks a valid currency from dropdown: warning disappears, save re-enables automatically
  - **Verify**: Edit a card with an orphaned currency code → warning shows, save is disabled → selecting a valid currency clears warning, enables save

- [x] 3.5 Update `CardListScreen.kt` to pass `currencyCodes` to `CardFormDialog`.
  - Collect: `val currencyCodes by viewModel.currencyCodes.collectAsStateWithLifecycle()`
  - Add `availableCurrencies = currencyCodes` to both `CardFormDialog` invocations:
    - Add dialog (~line 191): add `availableCurrencies = currencyCodes,`
    - Edit dialog (~line 218): add `availableCurrencies = currencyCodes,`
  - **Verify**: `./gradlew :feature-card-management:compileDebugKotlin`

- [x] 3.6 Update `CardListUiState.kt` — `CardDisplayItem.currency: Currency` → `String`.
  - `CardDisplayItem.currency: Currency` → `currency: String` (line 57)
  - Remove `import com.vida.domain.model.Currency`
  - **Verify**: All callers resolve (CardListViewModel `toDisplayItem`, `CardListItem`, `FuentesScreen` — they'll need updates too)

- [x] 3.7 Update `CardListItem.kt` — `card.currency.code` → `card.currency`.
  - Line 179: `text = card.currency.code` → `text = card.currency` (currency is now a String code)
  - **Verify**: File compiles

## Phase 4: App module (FuentesScreen)

- [x] 4.1 Update `app/src/main/kotlin/com/vida/app/ui/FuentesScreen.kt`.
  - Collect `currencyCodes`: `val currencyCodes by cardListViewModel.currencyCodes.collectAsStateWithLifecycle()` (near line 168, after `bankNames`)
  - Pass `availableCurrencies = currencyCodes` to both `CardFormDialog` invocations:
    - Add dialog (line 381): add `availableCurrencies = currencyCodes,`
    - Edit dialog (line 525): add `availableCurrencies = currencyCodes,`
  - Fix `currency.code` references (now that currency is String):
    - Line 469: `"Moneda: ${card.currency.code}"` → `"Moneda: ${card.currency}"`
    - Line 701: `text = card.currency.code` → `text = card.currency`
  - **Verify**: `./gradlew :app:compileFreeDebugKotlin`

## Phase 5: Tests

- [x] 5.1 Create `data/src/test/kotlin/com/vida/data/db/Migration1314Test.kt`.
  - Mirrors `Migration1213Test.kt` pattern (version bounds only)
  - `assertEquals(13, MIGRATION_13_14.startVersion)`
  - `assertEquals(14, MIGRATION_13_14.endVersion)`
  - **Verify**: `./gradlew :data:testDebugUnitTest --tests "com.vida.data.db.Migration1314Test"`

- [x] 5.2 Update `data/src/test/kotlin/com/vida/data/mapper/CardMapperTest.kt`.
  - Replace all `Currency.CUP` → `"CUP"`, `Currency.USD` → `"USD"`, `Currency.MLC` → `"MLC"`, `Currency.EUR` → `"EUR"` in test cards
  - `"all currencies round trip"`: iterate `listOf("CUP", "USD", "MLC", "EUR", "XYZ")` (String codes, not enum values)
  - Note: `Money.of("0.00", Currency.CUP)` still needs Currency enum — those stay (Money still holds Currency enum for its own ops)
  - Remove `import com.vida.domain.model.Currency` if no remaining enum references
  - **Verify**: `./gradlew :data:testDebugUnitTest --tests "com.vida.data.mapper.CardMapperTest"`

- [x] 5.3 Update `data/src/test/kotlin/com/vida/data/repository/CardRepositoryImplTest.kt`.
  - `aCardEntity()`: `currency = "CUP"` (line 126)
  - `aCard()`: `currency = "CUP"` (line 135)
  - `observeBalance` test (line 92): `Money.of("100.00", Currency.CUP)` stays (Money still uses Currency enum) — but the `dao.getById` mock returns entity with `currency = "CUP"`, and the repo now resolves Currency from String internally. Adjust expected assertion if needed — but since the test already expects `Money.of("100.00", Currency.CUP)`, the assertion should be unchanged.
  - Remove `import com.vida.domain.model.Currency` if unused after changes (it IS still used for assertEquals in balance test)
  - **Verify**: `./gradlew :data:testDebugUnitTest --tests "com.vida.data.repository.CardRepositoryImplTest"`

- [x] 5.4 Search for remaining `Currency.` usages in card-related test files.
  - `grep` for `Currency\.` in `feature-card-management/src/test/` and `data/src/test/` 
  - Update any remaining references that refer to card's currency field (not Money.currency)
  - **Verify**: `./gradlew :feature-card-management:testDebugUnitTest :data:testDebugUnitTest`

## Phase 6: Verification

- [x] 6.1 Run full unit test suite: `./gradlew :domain:test :data:testDebugUnitTest :feature-card-management:testDebugUnitTest :app:testFreeDebugUnitTest`
  - All tests must pass. If any fail, fix before proceeding.

- [x] 6.2 Build the full APK: `./gradlew :app:assembleFreeDebug`
  - Build must succeed with no compilation errors.

- [x] 6.3 Manual spot-check on device/emulator (if available):
  - Add a new card: verify currency dropdown shows user currencies, select one, save succeeds
  - Edit a card: verify its currency is pre-selected in dropdown
  - Verify cards display currency code correctly in card list and FuentesScreen

## Notes

- **Currency enum stays**: `Money`, `Wallet`, `Stash`, `CurrencyDao`, and other non-card domain objects still use `Currency` enum. Only card-related files change.
- **No ViewModelTest file exists**: `feature-card-management/src/test/` has no `CardListViewModelTest.kt` — no test to create/update for ViewModel specifically.
- **Schema JSON regeneration**: Room KSP auto-generates `14.json` when `exportSchema = true` and a compilation runs. The file needs to be committed along with the code changes.
- **Migration strategy**: Since `Converters.fromCurrency` already serialized to TEXT codes, no on-disk data changes. The migration exists solely to satisfy Room's schema validation.
