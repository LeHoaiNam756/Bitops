# Bit-operation per-run rerun

- Corpus: `test_file_zip/bitops_experiment.zip` (50 methods)
- Coverage types: `STATEMENT`, `BRANCH`, `MCDC`
- Repetitions: 10 per method and coverage type (1,500 attempts)
- Z3 encoding: `BITVECTOR`
- Per-attempt timeout: 10 seconds
- Java runtime: OpenJDK 17

`all-runs.csv` contains one row per attempt. The coverage, time, memory, Z3 variable count,
and Z3 expression count columns are populated when an attempt completes. They remain empty for
timed-out or failed attempts; `status` and `error` describe those outcomes.

`memory_mb` is CT4J's reported allocation measurement, converted from bytes to MiB. The two Z3
count columns are totals across the solver samples recorded during that attempt; `z3_sample_count`
states how many samples contributed.

The three `*-summary.csv` files aggregate completed attempts by method. The three `*-runs.csv`
files are the coverage-specific sources used to construct `all-runs.csv`.
