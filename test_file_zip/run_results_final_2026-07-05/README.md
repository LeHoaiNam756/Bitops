# `final.zip` concolic rerun

- Input: `test_file_zip/final.zip`
- Encoding: `BITVECTOR`
- Repetitions: 10 per method and coverage type
- Methods: 43
- Coverage types: `STATEMENT`, `BRANCH`, `MCDC`
- Per-attempt timeout: 10 seconds
- Total attempts: 1,290

## Per-attempt files

- `statement-runs.csv`
- `branch-runs.csv`
- `mcdc-runs.csv`

Each file has 430 rows: 43 full method signatures times 10 numbered attempts. Columns include status,
coverage, time, memory, total Z3 variable/expression counts, Z3 sample count, driver failures, and error.
Metric columns are empty for timed-out or failed attempts.

## Per-method summaries

- `statement-summary.csv`
- `branch-summary.csv`
- `mcdc-summary.csv`

## Attempt status counts

| Coverage | Success | Success with driver errors | Timeout | Failed |
|---|---:|---:|---:|---:|
| Statement | 378 | 2 | 50 | 0 |
| Branch | 374 | 2 | 44 | 10 |
| MC/DC | 377 | 6 | 37 | 10 |

The `generated-output-*` directories preserve the raw concolic JSON and generated artifacts produced by
each coverage process. The CSV files are authoritative for the complete set of attempted runs because
they retain timeout and failure rows and distinguish overloaded methods by full signature.
