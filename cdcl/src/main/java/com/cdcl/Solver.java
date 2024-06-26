package com.cdcl;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import java.util.Random;




public class Solver {

    private static int verbosity = 1;

    final static String SATISFIABLE = "SATISFIABLE";
    final static String UNSATISFIABLE = "UNSATISFIABLE";
    
    private Formula formula;

    private List<Integer> partialAssignment = new ArrayList<Integer>();

    private TwoWatch twoWatch;

    
    private HashSet<Integer> conflictClause = null;

    private HashMap<Integer,List<Integer>> UnitToDecisions = new HashMap<>();


    public Solver(String path)throws Exception{

        formula = Formula_IO.ReadFormula(new FileReader(path)); 
        twoWatch = new TwoWatch(formula);

        for (Integer initial_unit : formula.getInitialUnits()) {
            UnitToDecisions.put(initial_unit, Arrays.asList(initial_unit));
        }

    }

    public Solver( Reader input_reader) throws Exception{
        formula = Formula_IO.ReadFormula(input_reader);
        twoWatch = new TwoWatch(formula);

        for (Integer initial_unit : formula.getInitialUnits()) {
            UnitToDecisions.put(initial_unit, Arrays.asList(initial_unit));
        }
    }




    public String Solve(){

        

        if (verbosity>0) System.out.println("INITIAL UNITS: " + formula.getInitialUnits().toString());

        UnitPropogate(formula.getInitialUnits());


        int decisionLevel = 0; 

        Stack<Integer> decisionStack = new Stack<>();



        do {
            
            // whilst there a conflict keep on backjumping units and reversing decisions

            if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT BEFORE: "+ partialAssignment.toString());
            
            while( conflictClause != null ){
                
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
                UnitPropogate(Arrays.asList(   -partialAssignment.remove( partialAssignment.size()-1 )  )); 

                if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER UP: " + partialAssignment.toString());


            }

            if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER: "+ partialAssignment.toString());


            if (partialAssignment.size() != formula.getNumVariables()){

                // make decision on unassigned variables
                Integer literal_to_add = Decide();

                // increase decision level 
                decisionLevel += 1;
                
                // record size of partial assignment BEFORE decision  in decision stack 
                decisionStack.add(partialAssignment.size()+1);

                // after having added a decision variable, update the unit_decision mapping, so that decisions are implied by themselves
                UnitToDecisions.put(literal_to_add, Arrays.asList(literal_to_add));


                if (verbosity>0) System.out.println("DECISION MADE: " + literal_to_add);

                // add to partial assignment
                UnitPropogate(Arrays.asList(literal_to_add));
                
                if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER DECISION AND UP: " + partialAssignment.toString());
            
            }


        } while ( partialAssignment.size() != formula.getNumVariables() | conflictClause!=null );

        
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
    private void UnitPropogate(List<Integer> initial_units){

        if(verbosity>0)outputUnitToDecision();

        List<Integer> implicate_clauses = new ArrayList<Integer>();

        List<Integer> units = new ArrayList<Integer>(initial_units);
        
        while ( !units.isEmpty() ){

            //check if unit is already assigned 
            // if it already is then just add the inferences 
            // otherwise add it to partial assignment, and clear its inferences before adding new ones



            Integer unit_to_add = units.remove(0);

            if (!partialAssignment.contains(unit_to_add)){
               
          
                // reset this units set of inference decisions because it is only just being added 
                UnitToDecisions.put(unit_to_add, new ArrayList<Integer>());


                partialAssignment.add(unit_to_add);
                conflictClause = twoWatch.UpdateWatchedLiterals(unit_to_add, partialAssignment, units, implicate_clauses);


            }


            // add inference decisions to unit_decision map
            for (Integer lit:  formula.getClauses().get(implicate_clauses.removeLast())) {
                UnitToDecisions.get(unit_to_add).addAll( UnitToDecisions.get(-lit) );
            }
            



            if ( conflictClause != null){
                return;
            }





            


        }

        


    }

    private void outputUnitToDecision(){
        System.out.println("Unit To Decisions");
        for( Map.Entry<Integer, List<Integer> > entry: UnitToDecisions.entrySet()){
            System.out.println("Literal: "+ entry.getKey() + "   Decisions: "+ entry.getValue());
        }
    }


    



    public static void main( String[] args) throws Exception{

    
  
        

        Solver solver = new Solver("/home/waqee/CDCL2/cdcl/dpll_tests/simple/prop_rnd_985636_v_6_c_25_vic_2_4.cnf");

        // Solver solver = new Solver(new InputStreamReader(System.in));

        System.out.println(solver.Solve());
    

    } 



}




