# card-currency-dynamic — Delta Spec

## ADDED Requirements

| # | Requirement | Sc |
|---|-------------|----|
| R-001 | **Dropdown replaces FilterChip**. Currency selector MUST use `ExposedDropdownMenuBox` + `OutlinedTextField` (read-only, trailing icon) + `ExposedDropdownMenu` + `DropdownMenuItem`, matching bank dropdown pattern. `Currency.entries` FilterChip row MUST be removed. | 2 |
| R-002 | **Dropdown from user currencies**. Items MUST come from `ListCurrencies` (only currencies in user's table), NOT from `Currency` enum. | 2 |
| R-003 | **Required field**. Save MUST be disabled when no currency is selected. No "(none)" option. | 1 |
| R-004 | **Add card defaults**. New card: MUST default to first currency in user's list, or `"CUP"` if empty. `onSave` emits code as `String`. | 2 |
| R-005 | **Edit card + orphan**. MUST init with card's stored code. If orphaned (not in user's list): no selection, warning "La moneda asignada ya no existe. Seleccione otra.", Save disabled until valid currency picked. | 3 |
| R-006 | **Migration v13→v14**. `CardEntity.currency`: `Currency`→`String` (TEXT, no converter). Converters already stored codes; no data transform. All codes MUST survive. | 2 |
| R-007 | **Data layer String**. `CardRepository.addCard`/`updateCard` MUST accept `currency: String`. `CardMapper` MUST map String→String directly. | 2 |
| R-008 | **Backward compatible**. Pre-migration cards (CUP, USD, MLC, EUR, custom) MUST display/function unchanged. | 1 |
| R-009 | **No enum in card domain**. `Card.currency` MUST be `String`. `Card.kt` SHALL NOT import `Currency` enum. `CardDisplayItem` also `String`. | 1 |

## Scenarios

### R-001
- GIVEN dialog open / WHEN tap currency field / THEN menu expands; select code → collapses. Uses `TrailingIcon` + `.menuAnchor()` like bank.
- GIVEN dialog rendered / THEN no `Currency.entries.forEach { FilterChip }` row.

### R-002
- GIVEN user has CUP, USD, EUR / WHEN dialog opens / THEN dropdown shows exactly those three.
- GIVEN user added `MYCUSTOM` / WHEN dialog opens / THEN `MYCUSTOM` appears alongside built-in codes.

### R-003
- GIVEN no currency selected / THEN Save disabled regardless of other validations.

### R-004
- GIVEN user currencies [USD, EUR, CUP] / WHEN add dialog opens / THEN USD pre-selected. Save emits `"USD"`.
- GIVEN zero currencies / WHEN add dialog opens / THEN `"CUP"` pre-selected.

### R-005
- GIVEN card `currency = "USD"` and USD in user's list / WHEN edit opens / THEN USD pre-selected.
- GIVEN card `currency = "DELETED"` not in list / WHEN edit opens / THEN no selection, warning shown, Save disabled.
- GIVEN orphan warning visible / WHEN user picks valid currency / THEN warning gone, Save enabled.

### R-006
- GIVEN v13 DB with codes CUP, USD, MLC, EUR / WHEN migration 13→14 / THEN all rows survive; column TEXT, no converter.
- GIVEN migration test / WHEN migration runs + schema validates / THEN version=14, codes intact.

### R-007
- GIVEN `addCard(Card(currency="EUR"))` / WHEN persisted / THEN `CardEntity.currency` is `"EUR"` (String, no enum).
- GIVEN entity with `currency="USD"` / WHEN mapped / THEN domain `Card.currency` is `"USD"` (String).

### R-008
- GIVEN pre-migration card `MLC` / WHEN viewed post-migration / THEN displays `MLC` unchanged.

### R-009
- GIVEN codebase post-change / THEN `Card.currency` is `String`, zero `Currency` enum imports in `Card.kt`.
