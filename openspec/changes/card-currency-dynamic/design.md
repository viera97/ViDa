# Design: card-currency-dynamic

## Architecture overview

The card form's currency selector moves from a hardcoded `Currency.entries` `FilterChip` row to a dynamic `ExposedDropdownMenuBox` (matching the bank dropdown at `CardFormDialog.kt:143–171`). The dropdown items come from `ListCurrencies` → `currencyCodes` `StateFlow`. On the data layer, `CardEntity.currency` changes from `Currency` enum to `String` — since `Converters.kt` already serializes `Currency.code` as TEXT, the on-disk data is already string codes. A no-op v13→v14 migration satisfies Room schema validation. The `Currency` enum stays, used by `Money` and elsewhere.

## Files to modify

### domain/ (MODIFY)

| Path | Type | What changes |
|------|------|-------------|
| `domain/.../model/Card.kt` | MODIFY | `currency: Currency` → `currency: String`. Remove `Currency` import. |

### data/ (MODIFY + CREATE)

| Path | Type | What changes |
|------|------|-------------|
| `data/.../db/entity/CardEntity.kt` | MODIFY | `currency: Currency` → `currency: String`. Remove `Currency` import. |
| `data/.../mapper/CardMapper.kt` | MODIFY | `toDomain`/`toEntity`: pass `currency` as `String` directly, no enum conversion. Remove `Currency` import. |
| `data/.../repository/CardRepositoryImpl.kt` | MODIFY | `observeBalance`: read `currency` as `String`, convert via `Currency.values().firstOrNull { it.code.equals(code, true) } ?: Currency.CUP` for `Money` construction. Remove `Currency` import usage. |
| `data/.../db/AppDatabase.kt` | MODIFY | Bump `version` 13→14. Add `MIGRATION_13_14` to migration chain. |
| `data/.../db/Migration1314.kt` | CREATE | No-op migration: 13→14 marker — column already stores TEXT codes via `Converters.fromCurrency`; no ALTER TABLE needed. |

### feature-card-management/ (MODIFY)

| Path | Type | What changes |
|------|------|-------------|
| `feature-card-management/.../CardFormDialog.kt` | MODIFY | Parameters: `initialCurrency: String = "CUP"`, `availableCurrencies: List<String>`, `onSave` emits `String`. Replace `Currency.entries` `FilterChip` row (lines 192–201) with `ExposedDropdownMenuBox` + `OutlinedTextField` (read-only, trailing icon) + `ExposedDropdownMenu` + `DropdownMenuItem` per code. Orphan detection: if `initialCurrency` not in `availableCurrencies`, leave dropdown unselected, show `Text("La moneda asignada ya no existe. Seleccione otra.")` in red below dropdown, disable save. Remove `Currency` import. |
| `feature-card-management/.../CardListViewModel.kt` | MODIFY | Add `ListCurrencies` to constructor. Add `currencyCodes: StateFlow<List<String>> = listCurrencies().map { it.map { ci -> ci.code } }.stateIn(...)`. Update `onAdd`/`onEdit`: `currency: Currency` → `currency: String`. For `Money` construction, resolve code via `Currency.values().firstOrNull { it.code.equals(currency, true) } ?: Currency.CUP`. Update `toDisplayItem`/`toDisplayItemError`: `currency = currency` (already String, was enum). |
| `feature-card-management/.../CardListUiState.kt` | MODIFY | `CardDisplayItem.currency: Currency` → `currency: String`. Remove `Currency` import. |
| `feature-card-management/.../CardListScreen.kt` | MODIFY | Collect `currencyCodes` from ViewModel. Pass `availableCurrencies = currencyCodes` to `CardFormDialog` (add and edit). |
| `feature-card-management/.../CardListItem.kt` | MODIFY | `card.currency.code` → `card.currency` (already String display at line 179). |

### app/ (MODIFY)

| Path | Type | What changes |
|------|------|-------------|
| `app/.../ui/FuentesScreen.kt` | MODIFY | Collect `currencyCodes` from `cardListViewModel`. Pass `availableCurrencies = currencyCodes` to `CardFormDialog` (add at ~line 381, edit at ~line 525). `card.currency.code` → `card.currency` (line 469, 701). |

### Tests (NEW + MODIFY)

| Path | Type | What changes |
|------|------|-------------|
| `data/src/test/.../db/Migration1314Test.kt` | CREATE | Version bounds: `assertEquals(13, MIGRATION_13_14.startVersion)`, `assertEquals(14, ...endVersion)`. Mirrors `Migration1213Test` pattern. |
| `data/src/test/.../mapper/CardMapperTest.kt` | MODIFY | Replace `Currency.CUP`/`Currency.USD`/`Currency.MLC`/`Currency.EUR` with `"CUP"`/`"USD"`/`"MLC"`/`"EUR"`. Update "all currencies round trip" to iterate string codes: `listOf("CUP","USD","MLC","EUR","CUSTOM")`. |
| `data/src/test/.../repository/CardRepositoryImplTest.kt` | MODIFY | `aCardEntity()`: `currency = "CUP"`; `aCard()`: `currency = "CUP"`. |

## Key interactions

### Add card
1. User taps "+" → `showAddDialog = true` → `CardFormDialog` renders.
2. ViewModel provides `currencyCodes` (from `ListCurrencies` Flow): e.g. `["CUP","USD","EUR"]`.
3. Dropdown shows those items. First item pre-selected (or `"CUP"` if empty list).
4. User picks `EUR` → dialog state updates. Save enabled only when currency is selected + other validations pass.
5. `onSave` emits `"EUR"` as `String`. ViewModel constructs `Card(currency = "EUR")`.
6. `CardRepositoryImpl.upsert` → `CardMapper.toEntity(card)` → `CardEntity(currency = "EUR")` → Room stores `"EUR"` as TEXT.

### Edit card with orphan currency
1. User taps card with stored `currency = "XYZ"`.
2. ViewModel's `currencyCodes` list (from DB) does NOT contain `"XYZ"`.
3. `CardFormDialog` receives `initialCurrency = "XYZ"`, `availableCurrencies = ["CUP","USD"]`.
4. Dialog detects `"XYZ" !in availableCurrencies` → no item selected in dropdown, warning `Text` rendered: "La moneda asignada ya no existe. Seleccione otra." in `MaterialTheme.colorScheme.error`.
5. Save button disabled (no currency selected).
6. User picks valid currency → warning disappears → Save enabled → proceeds normally.

## Migration strategy

- **Version**: 13 → 14
- **Migration object**: `MIGRATION_13_14` — no-op body (no ALTER TABLE or data transforms).
- **Why no-op**: `Converters.fromCurrency(Currency): String` already serializes enum values as `code` (e.g. `"EUR"`). The column stores TEXT today. Changing `CardEntity.currency` from `Currency` to `String` makes Room see the column as plain TEXT instead of TEXT-via-converter — identical on disk.
- **What changes**: Room's generated schema JSON at v14 will show `"currency"` affinity as `TEXT` instead of `TEXT` via `TypeConverter`. The `MIGRATION_13_14` object exists so Room passes `MigrationTestHelper` validation — the body is empty because no SQL change is required.
- **Fallback**: if unexpected codes appear during mapping (codes absent from `Currency.values()`), `observeBalance` falls back to `Currency.CUP` for `Money` construction.

## Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Orphan currency makes card uneditable (save blocked) | Warning explains the situation; user picks replacement → save proceeds |
| `Money` construction fails for custom-currency codes | `Currency.values().firstOrNull { ... } ?: Currency.CUP` in ViewModel and `observeBalance` |
| Dropdown shows empty when user has zero currencies | `"CUP"` always seeded as default in `CardFormDialog` |
| `FuentesScreen` regression — dropdown missing | `currencyCodes` passed from `CardListViewModel` via `CardFormDialog` in both screens |
| Migration schema mismatch in tests | `MIGRATION_13_14` object registered even though body is no-op |
| `Currency` enum import ripple in 100+ files | Only card-related files change; `Currency` enum stays for `Money`, wallets, expenses, etc. |
