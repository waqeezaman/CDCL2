# CDCL SAT Solver

A Java implementation of a **Conflict-Driven Clause Learning (CDCL)** SAT solver. This solver determines the satisfiability of Boolean formulas in Conjunctive Normal Form (CNF) using advanced heuristics and optimization techniques.

## Overview

This project implements a modern SAT solver based on the CDCL algorithm, which combines systematic search with conflict analysis and learning. The solver is designed to efficiently handle large SAT instances through the use of:

- **Two-Watched Literals**: An efficient unit propagation mechanism
- **Conflict-Driven Learning**: Lemma generation and backjumping to avoid redundant search
- **VSIDS Heuristic**: Variable selection based on activity in recent conflicts
- **Clause Forgetting**: Memory management strategy for learned clauses
- **Geometric Restart Strategy**: Periodic search resets to escape poor decision sequences

## Performance Comparison with MiniSat

The table below shows the performance of this CDCL solver compared to **MiniSat** on the GoRRiLA-generated test suites:

| Test Set | Total SAT | Total UNSAT | Solver SAT Solved | MiniSat SAT Solved | Solver UNSAT Solved | MiniSat UNSAT Solved | Solver Avg Time (s) | MiniSat Avg Time (s) |
|----------|-----------|-------------|-------------------|-------------------|---------------------|----------------------|---------------------|----------------------|
| **Simple** | 34 | 66 | 34 (100%) | 34 (100%) | 66 (100%) | 66 (100%) | 0.0 | 0.0 |
| **Medium** | 129 | 81 | 129 (100%) | 129 (100%) | 81 (100%) | 81 (100%) | 0.0 | 0.0 |
| **Hard** | 61 | 70 | 47 (77%) | 61 (100%) | 38 (54%) | 70 (100%) | 2.4 | 0.0 |

**Key Observations:**
- **Perfect correctness on simple instances**: Solver matches MiniSat exactly (100/100 cases)
- **Perfect correctness on medium instances**: Solver matches MiniSat exactly (210/210 cases)
- **Limited performance on hard instances**: Solver solves 77% of SAT cases and 54% of UNSAT cases vs MiniSat's 100%
  - This represents 46 timeout cases (14 SAT + 32 UNSAT) out of 131 total
  - MiniSat solves hard instances in negligible time (0.0s) while this solver takes ~2.4s on solved cases

## Building the Project

### Prerequisites
- Java Development Kit (JDK) 11 or higher
- Maven 3.6 or higher

### Compile

Using Maven:
```bash
mvn clean compile
```

Build a JAR file:
```bash
mvn clean package
```

This creates an executable JAR at `target/cdcl.jar`.

## Running the Solver

### Basic Usage

Run the compiled JAR:
```bash
java -jar target/cdcl.jar -p <path-to-cnf-file>
```

### Output

The solver outputs either:
- `SATISFIABLE` - if the formula is satisfiable
- `UNSATISFIABLE` - if the formula is unsatisfiable

## Command Line Arguments

The solver supports the following options:

| Option | Short | Long | Description | Default |
|--------|-------|------|-------------|---------|
| **Path** | `-p` | `--path` | REQUIRED: Path to CNF file | - |
| **VSIDS Decay Rate** | `-vdr` | `--vsids-decay-rate` | Decay rate for VSIDS scores | 0.95 |
| **VSIDS Bump Rate** | `-vbr` | `--vsids-bump-rate` | Bump rate for conflicting variables | 1.2 |
| **VSIDS Decay Interval** | `-vdi` | `--vsids-decay-interval` | Conflicts between decay operations | 1 |
| **VSIDS Random Prob** | `-vrdp` | `--vsids-random-decision-probability` | Probability of random variable selection | 0.1 |
| **Restart Rate** | `-rgr` | `--restart-geometric-rate` | Geometric restart rate | 2 |
| **Decision Function** | `-df` | `--decision-function` | Variable selection strategy (e.g., "vsids") | vsids |
| **Learned Clause Max** | `-lcm` | `--learnt-clauses-maximum` | Maximum learned clauses before forgetting | 1000 |
| **Seed** | `--seed` | `--seed` | Random seed for reproducibility | random |
| **Verbosity** | `-v` | `--verbosity` | Enable verbose output | 0 |
| **Help** | `-h` | `--help` | Display help message | - |

### Example Commands

Solve a SAT instance:
```bash
java -jar target/cdcl.jar -p formulas/example.cnf
```

Solve with custom VSIDS parameters:
```bash
java -jar target/cdcl.jar -p formulas/example.cnf -vbr 1.5 -vdr 0.9
```

Solve with geometric restart rate of 1.5:
```bash
java -jar target/cdcl.jar -p formulas/example.cnf -rgr 1.5
```

Enable verbose output and set a random seed:
```bash
java -jar target/cdcl.jar -p formulas/example.cnf -v --seed 42
```

## Input Format

The solver reads formulas in the DIMACS CNF format:

```
c This is a comment
c
p cnf 3 2
1 -3 0
2 3 -1 0
```

- First line: `p cnf <num_variables> <num_clauses>`
- Following lines: Clauses with literals separated by spaces, ending with 0
- Comments start with 'c'

## Implementation Details

### Two-Watched Literals

The **two-watched literals** technique is a core optimization for unit propagation:

- For each clause, we maintain pointers to two literals (the "watched literals")
- When a literal becomes false, we only need to check clauses where its negation is watched
- We attempt to find a new watched literal that is either:
  - **Already assigned to true** → clause is satisfied, no action needed
  - **Unassigned** → replace the false watched literal with this one
  - **No suitable literal** → the other watched literal becomes a unit (must be true)

This reduces unit propagation from O(n) to O(1) amortized complexity per assignment.

**Data Structures:**
- `Literal_To_Clause`: Maps each literal to clauses where it is watched
- `Clause_To_Literal`: Maps each clause to its two watched literals

### Conflict Analysis and Learning

When a conflict occurs (a clause with all literals assigned to false), the solver performs **conflict analysis**:

1. **Start from conflict clause**: Add all literals whose negations were implied by recent decisions
2. **Trace implications backward**: Follow the implication graph to find responsible decision variables
3. **Generate learned clause**: The learned clause contains the negations of decision variables that caused the conflict
4. **Unit propagation**: The learned clause immediately implies the negation of one decision variable

**Benefits:**
- Prevents the solver from making the same mistakes again
- Provides a "shortcut" in the search space
- Can learn dependencies that weren't explicit in the original formula

### Backjumping

Traditional backtracking reverts one decision level at a time. This solver implements **conflict-driven backjumping**:

- Analyzes which decision variables directly caused the conflict
- Jumps back to the second-most-recent responsible decision level
- Immediately units-propagates the learned clause
- Continues from the correct search state

This is much more efficient than chronological backtracking, especially on instances with deep decision trees.

### VSIDS Heuristic

**Variable State Independent Decaying Sum (VSIDS)** is a dynamic variable selection heuristic:

**How it works:**
1. **Initialization**: Each variable gets a score equal to its occurrence count in the formula
2. **Bumping**: When a variable appears in a conflict, multiply its score by `vsidsBumpRate` (default: 1.2)
3. **Decaying**: Periodically (every `vsidsDecayInterval` conflicts), divide all scores by `vsidsDecayRate` (default: 0.95)
4. **Decision**: Select the unassigned variable with the highest score; with probability `vsidsRandomDecideProbability`, select randomly instead

**Why it's effective:**
- Recently involved variables are more likely to appear in future conflicts
- Decay prevents old conflicts from dominating forever
- Random decisions add diversity and help escape local patterns


### Clause Forgetting

As the solver learns more clauses, memory can become a bottleneck. The **forgetting strategy** manages clause retention:

**Implementation:**
- Set a limit on learned clauses: `numberOfAllowedLearntClauses` (default: 1000)
- When the limit is exceeded:
  - Remove all learned clauses
  - Reset the formula to its original size
  - Continue solving with a fresh start

**Benefits:**
- Prevents memory exhaustion on large instances
- Acts as an implicit restart mechanism
- Works in conjunction with the explicit restart strategy

### Restart Strategy

The solver periodically restarts the search to escape poor decision sequences:

- **Geometric progression**: Restarts occur after $`rgr^n`$ conflicts, where $`n`$ increments at each restart
- **Default rate**: `restartGeometricRate = 2` (restarts after 1, 2, 4, 8, 16, ... conflicts)
- **Preserved information**: VSIDS scores and learned clause activity are retained across restarts
- **Effect**: Allows the solver to explore different regions of the search tree while learning from previous attempts

## Algorithm Flow

```
1. Unit Propagate initial unit clauses
2. MAIN LOOP:
   a. While conflicts exist:
      i.   Analyze conflict and learn new clause
      ii.  Backjump to appropriate level
      iii. Unit propagate learned clause
   b. Decay VSIDS scores (periodically)
   c. Check for geometric restart condition
   d. Forget learned clauses if limit exceeded
   e. Make decision on unassigned variable (using VSIDS heuristic)
   f. Unit propagate decision
3. Return solution when all variables are assigned with no conflicts
```

## Testing

Note in order to test, comment out: 

```
    //Include this function for general purposes 
    // Uses command line arguments to set config and read formula from file path provided in arguments
    public static void main( String[] args) throws Exception{       
        processArgs(args);

        if (verbosity>0) outputConfig();

        Formula formula = Formula_IO.ReadFormula(new FileReader(formulaPath));
   
        if (Solver.getSatisfiable(formula)){ 
            System.out.println(SATISFIABLE);
        }
        else{
            System.out.println(UNSATISFIABLE);
        }
    } 
```

And uncomment 
```
   // Include this function for testing purposes
    public static void main( String[] args) throws Exception{
        if (verbosity>0) System.out.println("SEED : " + seed );

        Formula formula = Formula_IO.ReadFormula(new InputStreamReader(System.in));
   
        if (Solver.getSatisfiable(formula)){
            System.out.println(SATISFIABLE);
        }
        else{
            System.out.println(UNSATISFIABLE);
        }
        
    } 
```

And then recompile the project. 

This is done so that the input can be piped from g-test. 

TODO: improve interface to allow better more convenient use with evaluation framework.


The project includes test suites in the `dpll_tests/` directory with three difficulty levels:

### Automated Test Suite with g-test

The project includes a `g-test` script for running comprehensive test suites and comparing results with reference solvers (e.g., MiniSat).

**Prerequisites:**
- The `gorrila` test harness must be available (included in the project)
- Reference results for comparison (stored in `dpll_tests/*.res` files)

**Running the full test suite:**

```bash
# Make g-test executable
chmod +x g-test

# Test on simple instances
./g-test simple out_dir ./test.sh

# Test on medium instances
./g-test medium out_dir ./test.sh

# Test on hard instances
./g-test hard out_dir ./test.sh
```

The `g-test` script takes three arguments:
- `<set>`: Problem difficulty level (`simple`, `medium`, or `hard`)
- `<out_dir>`: Output directory for results
- `<solver-name>`: Name/path of the solver (e.g., `cdcl.jar` or `java -jar target/cdcl.jar`)

**Output:**

After running g-test, you'll find:
- `<solver-name>-<set>.res`: Results file comparing your solver against reference results
- `<solver-name>-<set>.log`: Detailed log file with solver output and timing information


The results files compare your solver's output and performance against the reference solver results stored in `dpll_tests/minisat-*.res`.

## Test Problem Generation

The test problems in the `dpll_tests/` directory were generated using **GoRRiLA v0.3** (The University of Manchester), a random problem generator for both linear arithmetic and propositional logic. Each test case is a randomly generated 3-SAT instance in DIMACS CNF format.

Todo: It is worth looking into generating problems with more than 3 variables in a single clause, this will allow for a more robust comparison between our solver and minisat.

### Filename Encoding

Each test filename encodes the generation parameters:

```
prop_rnd_[SEED]_v_[VARS]_c_[CLAUSES]_vic_[MIN]_[MAX].cnf
```

| Parameter | Example | Meaning |
|-----------|---------|---------|
| `prop_rnd` | - | Propositional logic, random generation |
| `[SEED]` | `129735` | Random seed for reproducibility |
| `v_[VARS]` | `v_3` | Number of variables |
| `c_[CLAUSES]` | `c_12` | Number of clauses |
| `vic_[MIN]_[MAX]` | `vic_1_4` | Clause-to-variable ratio bounds |

For example: `prop_rnd_129735_v_3_c_12_vic_1_4.cnf` is a random 3-SAT instance with:
- 3 variables
- 12 clauses
- Clause-to-variable ratio between 1:4 and 4:1
- Generated with seed 129735

### Test Difficulty Levels

The test suite is organized into three categories reflecting increasing difficulty:

- **Simple**: Small instances (few variables and clauses)
- **Medium**: Moderate complexity instances
- **Hard**: Large, complex instances designed to challenge solvers

Each category contains hundreds of test instances with different random seeds to ensure diverse problem characteristics.

### Reference Benchmarks

The `dpll_tests/minisat-*.res` files contain evaluation results from **MiniSat**, a reference SAT solver. These serve as performance benchmarks when running the `g-test` script, allowing you to compare your solver's performance and correctness against an established SAT solver.



## API Usage

The solver can also be used programmatically:

```java
// Read a formula
Formula formula = Formula_IO.ReadFormula(new FileReader("example.cnf"));

// Check satisfiability
boolean isSat = Solver.getSatisfiable(formula);

// Get a solution
List<Integer> solution = new Solver(formula).Solve();

// Get multiple solutions
List<List<Integer>> allSolutions = Solver.getAllSolutions(formula);
int N = 5;
List<List<Integer>> firstN = Solver.getNSolutions(formula, N);
```

To see examples of how this library has been applied in other projects see: 
- https://github.com/waqeezaman/sodoku_solver
- https://github.com/waqeezaman/Grid_Generator

## References

### CDCL Algorithm & SAT Solving
- Marques-Silva, J., & Sakallah, K. A. (1999). "GRASP: A search algorithm for propositional satisfiability." *IEEE Transactions on Computers*, 48(5), 506-521.
- Moskewicz, M. W., Madigan, C. F., Zhao, Y., Zhang, L., & Malik, S. (2001). "Chaff: Engineering an efficient SAT solver." *Proceedings of the 38th Design Automation Conference (DAC)*, 530-535.

### Key Techniques
- **Two-Watched Literals**: Moskewicz et al. (2001)
- **VSIDS Heuristic**: Variable State Independent Decaying Sum, introduced in Chaff solver
- **Conflict-Driven Clause Learning**: Marques-Silva & Sakallah (1999), further refined by many subsequent solvers
- **Backjumping & Non-Chronological Backtracking**: Standard in modern CDCL solvers

### Test Problem Generation
- **GoRRiLA v0.3**: Random problem generator developed by Konstantin Korovin and Andrei Voronkov, The University of Manchester
  - Generates random 3-SAT instances in DIMACS CNF format
  - Allows parametric control over problem difficulty and structure
  - Available in the `gorrila_v0.3/` directory

### Related Solvers
- **MiniSat**: Eén, N., & Sörensson, N. (2003). "An extensible SAT-solver." *Theory and Applications of Satisfiability Testing*, 502-518.
  - Reference solver used for benchmark comparison in this project

## References

```bibtex
@InProceedings{10.1007/978-3-540-24605-3_37,
author="E{\'e}n, Niklas
and S{\"o}rensson, Niklas",
editor="Giunchiglia, Enrico
and Tacchella, Armando",
title="An Extensible SAT-solver",
booktitle="Theory and Applications of Satisfiability Testing",
year="2004",
publisher="Springer Berlin Heidelberg",
address="Berlin, Heidelberg",
pages="502--518",
abstract="In this article, we present a small, complete, and efficient SAT-solver in the style of conflict-driven learning, as exemplified by Chaff. We aim to give sufficient details about implementation to enable the reader to construct his or her own solver in a very short time. This will allow users of SAT-solvers to make domain specific extensions or adaptions of current state-of-the-art SAT-techniques, to meet the needs of a particular application area. The presented solver is designed with this in mind, and includes among other things a mechanism for adding arbitrary boolean constraints. It also supports solving a series of related SAT-problems efficiently by an incremental SAT-interface.",
isbn="978-3-540-24605-3"
}

@misc{Korovin2008,
  author = {Korovin, Konstantin and Voronkov, Andrei},
  title = {{GoRRiLA}: A Random Problem Generator for Propositional Logic and Linear Arithmetic},
  year = {2008},
  organization = {The University of Manchester},
}
```

