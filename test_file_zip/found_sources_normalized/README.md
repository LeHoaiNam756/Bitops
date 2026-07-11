# Normalized found-source bit-operation corpus

This folder is the runnable CT4J version of `../found_sources/methods.csv`.

- Source file: `found/FoundSourcesNormalized.java`
- ZIP for CT4J: `../found_sources_normalized.zip`
- Method count loaded by CT4J: 50
- Normalization: package/imports removed, object dependencies replaced with primitive or array
  parameters, array indices bounded where needed, and source-specific names prefixed by project.

This corpus is separate from `../bitops_experiment.zip`. The old `bitops_experiment.zip`
contains the earlier `final/` folder plus only seven normalized found-source additions.

Run example:

```bash
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 \
PATH=/usr/lib/jvm/java-17-openjdk-amd64/bin:$PATH \
java \
  -Xms256m -Xmx3g \
  -DzipPath=test_file_zip/found_sources_normalized.zip \
  -Dcoverage=STATEMENT \
  -Druns=10 \
  -DtimeoutSeconds=10 \
  -Dct4j.max.generated.array.length=128 \
  -DoutputCsv=test_file_zip/experiment_results/found_sources_statement.csv \
  -DrunOutputCsv=test_file_zip/experiment_results/found_sources_statement_runs.csv \
  -cp "target/classes:$(cat target/runtime-classpath.txt)" \
  core.generation.CorpusExperimentRunner
```

Repeat with `-Dcoverage=BRANCH` and `-Dcoverage=MCDC` for the other coverage types.
