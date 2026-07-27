# Proposal: card-currency-dynamic

## Intent
Dynamic currency dropdown in card form instead of hardcoded `Currency` enum `FilterChip` row.

## Problem
The `CardFormDialog` (lines 192–201) renders a hardcoded `Currency.entries.forEach { FilterChip(...) }` row using the old `Currency` enum (CUP, USD, MLC, EUR). Since the `currency-management` change was completed (SDD), users can now add/edit/delete arbitrary currencies via `CurrencyInfo`. The card form must reflect **only** currencies the user has added — not a static enum.

Current state (code reference):
- `CardEntity.currency: Currency` — stored as enum code via `Converter.kt` (`fromCurrency`/`toCurrency` using `value.code`)
- `Card.currency: Currency` — domain model field
- `CardFormDialog` parameter: `initialCurrency: Currency = Currency.CUP`, `onSave` passes `Currency`
- `CardListViewModel.onAdd/onEdit` — accept `currency: Currency`, construct `Card` and `Money` with it
- `CardDisplayItem.currency: Currency` — used by `FuentesScreen` to render `"Moneda: ${card.currency.code}"`
- `CardRepositoryImpl.observeBalance` — uses `dao.getById(id)?.currency ?: Currency.CUP`
- 100+ files import `com.vida.domain.model.Currency` — enum is deeply embedded in `Money`, expenses, transfers, wallets, etc.

## Scope

### In
- Replace `Currency.entries` `FilterChip` row in `CardFormDialog` with an `ExposedDropdownMenuBox` (same pattern as bank selection at lines 143–171)
- `CardEntity.currency` → `String` (code, e.g. `"CUP"`, `"EUR"`, `"MYCUSTOM"`)
- `Card.currency` → `String` (domain model)
- `CardDisplayItem.currency` → `String`
- Migration v13 → v14: column already stores codes via TypeConverter; promote to plain `TEXT` without converter dependency
- Add `currencyCodes: StateFlow<List<String>>` to `CardListViewModel` via `ListCurrencies().map { codes }`
- Update `CardFormDialog` signature: `initialCurrency: String = "CUP"`, `onSave` emits `String`
- Update `CardMapper`, `CardRepositoryImpl.observeBalance`, `FuentesScreen`, and tests

### Out
- Bank selection (unchanged)
- Wallet, expense, income, transfer, stash currency fields (still use `Currency` enum via `Money` and their entities)
- Adding new currencies via this change (already exists via `CurrencyInfo` management)
- Unrelated cleanup

## Approach
Replace the `Currency.entries` `FilterChip` row in `CardFormDialog` with the same `ExposedDropdownMenuBox` + `ExposedDropdownMenu` + `DropdownMenuItem` pattern already used for bank selection. The dropdown is fed by a new `currencyCodes` `StateFlow` in `CardListViewModel`, derived from `ListCurrencies`.

On the data layer, `CardEntity.currency` changes from `Currency` enum to `String`. Since `Converters.kt` already serializes `Currency` as `value.code` (TEXT), the on-disk data is already string codes — no SQL data transformation is needed in the migration. The Room schema version is bumped to 14 and a no-op migration (or just the version marker) registers the change.

The domain model `Card.currency` changes from `Currency` enum to `String`. `Money.currency` stays as `Currency` enum (it is the balance currency, a separate concern from card display). In `onAdd`/`onEdit`, the user-supplied code is resolved to a `Currency` enum for `Money` construction via `Currency.fromCode(code) ?: Currency.CUP` with a fallback for custom codes.

The `observeBalance` method in `CardRepositoryImpl` currently reads `entity.currency` (enum) to construct `Money`. After the change, it reads the String code and passes it through `Currency.fromCode()` with a `CUP` fallback.

## Decision (locked)
**Approach A** — confirmed by user; not reopen for discussion. This proposal implements exactly:
- `CardEntity.currency`: `Currency` → `String`
- Migration v13 → v14
- `FilterChip` → `ExposedDropdownMenu` in `CardFormDialog`

## Open questions
1. **Empty selection vs. required**: Should the dropdown allow no currency selected (empty/null state), or must the user pick exactly one? Current behavior: `Currency.CUP` is the default. Proposed: keep a required default — first currency in the user's list, or `"CUP"` if the list is empty.

2. **Orphan currency handling**: When editing a card whose saved currency code no longer exists in the `currencies` table (user deleted the currency after assigning it to a card):
   - **(a)** Show a warning text below the dropdown: "Moneda 'XYZ' ya no está disponible" — block save until a valid currency is selected.
   - **(b)** Silently fall back to `CUP` in the dropdown but preserve the orphan code in the entity until the user edits.
   - **(c)** Show the orphan code in the dropdown as an extra disabled item with a warning, allow selecting a replacement.
   - Recommended: **(a)** — clearest UX, no silent data corruption.

3. **Card balance vs. display currency mismatch**: If the user picks `EUR` for a card but the card's `initialBalanceCurrency` was `CUP`, the `Money` in `balance` will use `Currency.CUP` (string → enum conversion) while the card display shows `EUR`. Is this acceptable? The balance formatting in `CardDisplayItem` uses `balance.currency.symbol`, which would show `$` (CUP) while the card badge shows `EUR`. This is a pre-existing concern — `initialBalanceCurrency` and `currency` are already separate fields. To be addressed in design if needed.

4. **`Currency` enum deprecation path**: After this change, `Currency` enum is still used by `Money`, wallets, expenses, transfers, etc. Should we annotate it with `@Deprecated` with a message pointing to `CurrencyInfo`? Not in scope for this change, but noting for future.

## Acceptance criteria
1. **Add card**: dropdown shows only currencies the user has in the `currencies` table (built-in + custom)
2. **Edit card**: initial dropdown selection matches the card's stored currency code
3. **Migration**: existing cards at v13 keep their currency codes (stored as TEXT via converter, now read directly)
4. **FilterChip row removed** — the `Currency.entries` hardcoded row no longer appears
5. **Dropdown UX** matches bank dropdown: `OutlinedTextField` with trailing icon, `ExposedDropdownMenu`, `DropdownMenuItem` per currency code

## Files affected (preliminary)
Grouped by module:

### domain
| File | Change |
|------|--------|
| `domain/.../model/Card.kt` | `currency: Currency` → `currency: String` |

### data
| File | Change |
|------|--------|
| `data/.../db/entity/CardEntity.kt` | `currency: Currency` → `currency: String` |
| `data/.../db/AppDatabase.kt` | version 13→14, add `MIGRATION_13_14` to chain |
| `data/.../db/Migration1314.kt` | **new** — no-op or column-affinity migration |
| `data/.../mapper/CardMapper.kt` | remove `Currency` import, direct String mapping |
| `data/.../db/dao/CardDao.kt` | (no change expected) |
| `data/.../repository/CardRepositoryImpl.kt` | `observeBalance`: read String code, convert to enum for Money |
| `data/.../di/DatabaseModule.kt` | (no change expected — mapper is `object`) |

### feature-card-management
| File | Change |
|------|--------|
| `feature-card-management/.../CardFormDialog.kt` | parameter `Currency`→`String`, remove FilterChip row, add ExposedDropdownMenu |
| `feature-card-management/.../CardListViewModel.kt` | add `currencyCodes` StateFlow, update `onAdd`/`onEdit` signatures, `toDisplayItem` |
| `feature-card-management/.../CardListUiState.kt` | `CardDisplayItem.currency: Currency` → `String` |
| `feature-card-management/.../CardListScreen.kt` | pass `currencyCodes` to `CardFormDialog` |

### app
| File | Change |
|------|--------|
| `app/.../ui/FuentesScreen.kt` | `card.currency.code` → `card.currency` (already a code); pass `currencyCodes` to `CardFormDialog` |

### tests
| File | Change |
|------|--------|
| `data/.../db/Migration1314Test.kt` | **new** — version bounds check |
| `data/.../mapper/CardMapperTest.kt` | update `Currency` references to `String` codes |
| `data/.../repository/CardRepositoryImplTest.kt` | update `Currency` references |
| (ViewModel/UI tests if any exist for `CardFormDialog`) | update if present |

## Risks
| Risk | Severity | Mitigation |
|------|----------|------------|
| Migration data loss if enum mapping is wrong | Low | Converters already store codes as TEXT; no data transformation needed |
| UI regression if dropdown style diverges from bank dropdown | Medium | Reuse the same `ExposedDropdownMenuBox` pattern exactly |
| Orphan currency UX confusing users | Medium | Block save + show warning (resolved in design — open question 2) |
| `Money` construction fails for custom currency codes | Medium | Fall back to `Currency.CUP` for `Money` construction; document that balance display shows CUP for custom-currency cards |
| 100+ files import `Currency` — ripple effect if enum removal is attempted | N/A (out of scope) | Only `Card.currency` and `CardDisplayItem.currency` change; `Currency` enum still used by Money everywhere else |

## Next phase
`spec` — write delta specs with requirements and scenarios for:
- Card form currency dropdown behavior (empty list, single item, many items)
- Migration v13→v14 correctness
- Edit flow with orphan currency code
- Add flow with dynamic currency list
