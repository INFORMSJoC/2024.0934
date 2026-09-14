[![INFORMS Journal on Computing Logo](https://INFORMSJoC.github.io/logos/INFORMS_Journal_on_Computing_Header.jpg)](https://pubsonline.informs.org/journal/ijoc)

# Urban Delivery with a Coordinated Fleet of Trucks and Drones

This archive is distributed in association with the [INFORMS Journal on
Computing](https://pubsonline.informs.org/journal/ijoc) under the [MIT License](LICENSE).

The software and data in this repository are a snapshot of the software and data
that were used in the research reported on in the paper
*Urban Delivery with a Coordinated Fleet of Trucks and Drones* by W. Zeng, H. Yang, H. Wang, and M. Qi.

## Cite

To cite the contents of this repository, please cite both the paper and this repository using their respective DOIs.

Paper DOI:

```text
https://doi.org/10.1287/ijoc.2024.0934
```

Code repository DOI:

```text
https://doi.org/10.1287/ijoc.2024.0934.cd
```

BibTeX for this repository snapshot:

```bibtex
@misc{[BibTeX-Key],
  author =        {Zeng, Wenjia and Yang, Hai and Wang, Hai and Qi, Mingyao},
  publisher =     {INFORMS Journal on Computing},
  title =         {{Urban Delivery with a Coordinated Fleet of Trucks and Drones}},
  year =          {2026},
  doi =           {10.1287/ijoc.2024.0934.cd},
  url =           {https://github.com/10.1287/ijoc.2024.0934},
  note =          {Available for download at https://github.com/10.1287/ijoc.2024.0934},
}
```

## Description

This repository contains the Java implementation of a concurrent JAMES algorithm 
for a same-day delivery problem with drone resupply. In the problem, a fleet of
trucks delivers parcels while drones resupply trucks at meeting points. The solution 
approach combines:

- a **label-setting algorithm** (`LabelingAlgorithm.java`) that solves the pricing
  subproblem — a shortest path problem with resource constraints over truck arcs and
  drone meeting arcs — using forward/backward labels and dominance rules;
- a **concurrent branch-and-bound** framework that branches on assigning a customer to
  a truck versus rejecting it, provided in three variants: deque-based
  (`BranchAndBound.java`), best-first search (`BranchAndBound_BFS.java`), and a
  concurrent depth-queue search (`BranchAndBound_CBFS.java`, the default);
- a **nearest-addition heuristic** (`NearestAdditionAlgorithm.java`) to construct the
  initial routes, with small TSP subproblems solved by Gurobi;
- a **random instance generator** (`GenerateInstance.java`) and a set of benchmark
  instances under `data/`.

## Repository Structure

```
.
├── README.md
├── AUTHORS
├── LICENSE
├── src/
│   ├── BranchAndBound.java            # Concurrent deep-first search branch-and-bound
│   ├── BranchAndBound_BFS.java        # Concurrent best-first search branch-and-bound
│   ├── BranchAndBound_CBFS.java       # Concurrent depth-queue branch-and-bound (default)
│   ├── LabelingAlgorithm.java         # Forward/backward labeling algorithm (pricing subproblem)
│   ├── NearestAdditionAlgorithm.java  # Nearest-addition initial-solution heuristic
│   ├── Instance.java                  # Instance data structures and file reader
│   ├── Node.java                      # Customer/node data structure
│   ├── Route.java                     # Route data structure
│   └── Truck.java                     # Truck data structure
├── scripts/
│   ├── GenerateInstance.java          # Random instance generator
│   └── TestMain.java                  # Main entry point (experiment driver)
├── data/                              # Benchmark instances (.txt)
└── results/                           # Output result files
```

## Requirements

- Java SE Development Kit (JDK) 8 or later.
- [Gurobi Optimizer](https://www.gurobi.com/) with a valid license, including the
  Gurobi Java library (`gurobi.jar`).

## Replicating Results

1. Compile the sources (replace `<path-to-gurobi.jar>` with your Gurobi installation path):

   ```bash
   javac -cp "<path-to-gurobi.jar>" src/*.java scripts/*.java
   ```

2. Run from the project root. On Windows the classpath separator is `;`, on Linux/macOS
   it is `:`:

   ```bash
   # Windows
   java -cp "<path-to-gurobi.jar>;src;scripts" TestMain

   # Linux/macOS
   java -cp "<path-to-gurobi.jar>:src:scripts" TestMain
   ```

By default `scripts/TestMain.java` reads the instance `data/A_4_50_3.txt` (see the
`dataFile` variable). To run a different instance, point `dataFile` to any `.txt` file
under `data/`. The branch-and-bound variant is selected in `scripts/TestMain.java`
by choosing which `BranchAndBound*` class to instantiate; the default is
`BranchAndBound_CBFS`.

## Results

After a run, the solution (lower and upper bounds, optimality gap, CPU time, branch
count, and the route of each truck) is printed to the console and also written to
`results/<instance-name>.txt`, where `<instance-name>` matches the input instance file
name.

## Originality Statement

All source code in this repository, including all scripts and implementation files, 
is original work developed by the authors specifically for this paper. No part of the 
code has been used in other projects by the authors or third parties.

## Data

The exact problem data used in the computational experiments are included under `data/`. 

## Support

For questions about the code, please contact the authors listed in `AUTHORS`.



