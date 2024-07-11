## CDCL


An implementation of the CDCL Algorithm in java. 


You can either use this SAT Solver directly through the command line interface, by calling the cdcl.jar file and providing arguments, or import it into your project and use the static methods provided in Solver.java.

The solver takes .cnf files in the DIMACS format.  


## Includes:  
Backtracking, after a conflict is found, backtracks multiple decision levels to the source of the conflict  
  
Clause Learning, after a conflict is found, adds a new clause to the clause set, based on the decisions that led to the conflict  
  
Restarts, resets the partial assignment, every a^n conflicts, where a is a parameter, and n is incremented after every restart   
  
A primitve forgetting strategy, forgets all learnt clauses, every time it restarts.   
  
Two Watch Literals, uses a two watch literal data structure to efficiently identify conflicts, units and unsatisfied clauses   

