# final2 concolic run

- Corpus: `test_file_zip/final2.zip`, created from `test_file_zip/final2`
- Methods loaded: 28
- Coverage types: `STATEMENT`, `BRANCH`, `MCDC`
- Repetitions: 10 per method and coverage type
- Total attempts: 840
- Per-attempt timeout: 60 seconds
- Encoding: `BITVECTOR`
- Runtime used: OpenJDK 17 via `/usr/lib/jvm/java-17-openjdk-amd64`

Files:

- `final2-runs.csv`: one row per attempt, including coverage percent, time, memory, variable count, and Z3 expression count.
- `final2-summary.csv`: aggregate row per coverage type and method.
- `smoke-runs.csv` and `smoke-summary.csv`: one-method smoke run used before the full run.

Verification:

- `final2-runs.csv`: 840 data rows plus header.
- `final2-summary.csv`: 84 data rows plus header.
- Status distribution: 840 `SUCCESS`, 0 failed or timed out attempts.
- Coverage distribution: 280 rows each for `STATEMENT`, `BRANCH`, and `MCDC`.
