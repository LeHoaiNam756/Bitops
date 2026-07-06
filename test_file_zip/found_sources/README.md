# Branch-heavy bit-operation sources

`methods.csv` contains 50 candidate methods selected using two mandatory criteria:

1. The method contains control flow (`if`, `switch`, `for`, `while`, `do`, or a conditional expression).
2. The method contains at least one bitwise or shift operator (`&`, `|`, `^`, `~`, `<<`, `>>`, or `>>>`).

The Java files are unmodified upstream sources with their original license headers. Before running them
through CT4J, extract the selected method and the constants or small helpers it requires, remove package
and annotation dependencies, and replace unsupported object parameters with primitive inputs where needed.
