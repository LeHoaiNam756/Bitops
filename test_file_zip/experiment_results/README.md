# Bit-operation concolic experiment

## Outputs

- `bitops-concolic-results.csv`: combined result, 150 data rows (50 methods x 3 coverage types).
- `final/statement.csv`, `final/branch.csv`, `final/mcdc.csv`: per-coverage results.
- `final/*.log`: raw execution logs.
- `../bitops_experiment.zip`: normalized 50-method input corpus.
- `../found_sources/methods.csv`: 50 internet-source candidates and bit operators.

## Configuration

- Coverage types: `STATEMENT`, `BRANCH`, and `MCDC`.
- Requested repetitions: 10 for every method and coverage type.
- Z3 encoding: `BITVECTOR`.
- Per-repetition timeout: 10 seconds.
- Maximum generated array length: 128.
- Java runtime: OpenJDK 17.
- Each coverage job used an isolated project copy and generated-driver directory.

The normalized corpus contains the 43 methods accepted from the supplied `final.zip` plus seven
dependency-reduced methods in `AdditionalBitOps.java`. Method identity includes class, name, and parameter
types, so overloaded methods cannot overwrite one another.

## Column definitions

- `completed_runs`: repetitions whose complete `generate()` call returned within the timeout.
- `average_coverage_percent`: arithmetic mean of `rawCoveragePercent()` over completed repetitions.
- `average_time_ms`: arithmetic mean of reported execution time over completed repetitions.
- `average_memory_mb`: arithmetic mean of reported memory bytes divided by 1,048,576.
- `average_variable_count`: mean number of unique free variables in a final Z3 assertion DAG.
- `average_z3_expression_count`: mean number of distinct AST nodes in a final Z3 assertion DAG.

The two Z3 metrics are sampled from `z3Solver.getAssertions()` immediately before `z3Solver.check()`.
They are averaged over every solver invocation made by completed repetitions. A zero means that no
pre-solve final assertion sample was produced, not necessarily that the Java method has no variables.

## Status interpretation

- `SUCCESS`: all 10 repetitions completed.
- `SUCCESS_WITH_DRIVER_ERRORS:N`: all repetitions returned, but concolic generation recovered from `N`
  concrete-driver failures internally.
- `PARTIAL:timeout`: some repetitions completed and at least one timed out.
- `FAILED:timeout`: no repetition completed within the timeout.
- Other `FAILED:*` values preserve the worker exception that prevented completion.

Completed repetition totals are 423 for statement coverage, 411 for branch coverage, and 408 for MCDC.
Two `Uft8.isWellFormedSlowPath` rows failed because CT4J could not compile its regenerated source for
branch and MCDC coverage; the statement run timed out. These failures are retained rather than replaced
with synthetic measurements.

## Validation

The combined CSV has exactly 50 rows per coverage type, identical 50-method sets across all types, and no
missing, `NaN`, or infinite values. Zero metric values on failed rows are sentinels because there were no
completed repetitions to average.
