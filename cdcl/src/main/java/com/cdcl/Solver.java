package com.cdcl;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.List;
import java.util.Stack;

import java.util.Random;




public class Solver {

    private static int verbosity = 0;

    final static String SATISFIABLE = "SATISFIABLE";
    final static String UNSATISFIABLE = "UNSATISFIABLE";
    
    private Formula formula;

    private List<Integer> partialAssignment = new ArrayList<Integer>();

    private TwoWatch twoWatch;

    



    public Solver(String path)throws Exception{

        formula = Formula_IO.ReadFormula(new FileReader(path)); 
        twoWatch = new TwoWatch(formula);

    }

    public Solver( Reader input_reader) throws Exception{
        formula = Formula_IO.ReadFormula(input_reader);
        twoWatch = new TwoWatch(formula);
    }




    public String Solve(){

        boolean conflictingAssignment = false;

        if (verbosity>0) System.out.println("INITIAL UNITS: " + formula.getInitialUnits().toString());

        conflictingAssignment = UnitPropogate(formula.getInitialUnits());


        int decisionLevel = 0; 

        Stack<Integer> decisionStack = new Stack<>();



        do {
            
            // whilst there a conflict keep on backjumping units and reversing decisions

            if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT BEFORE: "+ partialAssignment.toString());
            
            while( conflictingAssignment ){
                
                if (verbosity>0) System.out.println("ASSIGNMENT CONFLICTING");

                if( decisionLevel == 0) return UNSATISFIABLE;


                // analyse conflict 

                // reverse partial assignment by looking at its initial size in the decision stack 
                partialAssignment = partialAssignment.subList(0, decisionStack.pop());

                if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER TRIM: " + partialAssignment.toString());

                decisionLevel -= 1;

                // add learned clause to formula 

                // propagate new units made after
                
                // adding reverse of decision FOR NOW, and REMOVING decision HERE  
                conflictingAssignment = UnitPropogate(Arrays.asList(   -partialAssignment.remove( partialAssignment.size()-1 )  )); 

                if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER UP: " + partialAssignment.toString());


            }

            if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER: "+ partialAssignment.toString());


            if (partialAssignment.size() != formula.getNumVariables()){

                // make decision on unassigned variables
                int literal_to_add = Decide();

                // increase decision level 
                decisionLevel += 1;
                
                // record size of partial assignment BEFORE decision  in decision stack 
                decisionStack.add(partialAssignment.size()+1);

                if (verbosity>0) System.out.println("DECISION MADE: " + literal_to_add);

                // add to partial assignment
                conflictingAssignment = UnitPropogate(Arrays.asList(literal_to_add));
                
                if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER DECISION AND UP: " + partialAssignment.toString());
            
            }


        } while ( partialAssignment.size() != formula.getNumVariables() | conflictingAssignment );

        
        if (verbosity>0) System.out.println("FINAL ASSIGNMENT: " + partialAssignment.toString());

        return SATISFIABLE;
    }


    private int Decide(){

        Random rand = new Random();
        int random_literal;

        do{ 

            random_literal = rand.nextInt(-formula.getNumVariables(), formula.getNumVariables()+1 ) ;

        }
        while (partialAssignment.contains(random_literal) | partialAssignment.contains(-random_literal) | random_literal==0 );
        
        return random_literal;
    }



    /**
     * @param initial_units , units to propogate
     * @return returns true if a conlfict has occurred, false otherwise, also updates partial assignment 
     */
    private boolean UnitPropogate(List<Integer> initial_units){

        List<Integer> units = new ArrayList<Integer>(initial_units);
        
        while ( !units.isEmpty() ){

            Integer unit_to_add = units.remove(0);
            partialAssignment.add(unit_to_add);
            boolean conflict_occurred = twoWatch.UpdateWatchedLiterals(unit_to_add, partialAssignment, units);

            if ( conflict_occurred){
                return true;
            }


        }

        return false;


    }


    



    public static void main( String[] args) throws Exception{

    
  
        

        // Solver solver = new Solver("/home/waqee/CDCL2/cdcl/dpll_tests/medium/prop_rnd_523509_v_17_c_72_vic_3_3.cnf");

        Solver solver = new Solver(new InputStreamReader(System.in));

        System.out.println(solver.Solve());
    

    } 



}




