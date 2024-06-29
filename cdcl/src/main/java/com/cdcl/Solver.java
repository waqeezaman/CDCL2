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

    private static int verbosity = 0;
    private static int seed = 0; 
    private static Random rand;

    final static String SATISFIABLE = "SATISFIABLE";
    final static String UNSATISFIABLE = "UNSATISFIABLE";
    
    private Formula formula;

    private List<Integer> partialAssignment = new ArrayList<Integer>();

    private TwoWatch twoWatch;

    
    // private HashSet<Integer> conflictClause = null;
    private Integer conflictClause = null;

    private HashMap<Integer, List<Integer> > LiteralToImplicationClause = new HashMap<>(); 

    private HashMap<Integer,Integer> DecisionToLevel = new HashMap<Integer,Integer>();

    private Stack<Integer> decisionStack = new Stack<>();

    private Integer decisionLevel = 0;



    public Solver(String path)throws Exception{
        rand = new Random(seed);

        formula = Formula_IO.ReadFormula(new FileReader(path)); 
        twoWatch = new TwoWatch(formula);

       for (int i = -formula.getNumVariables(); i <= formula.getNumVariables(); i++) {
            if (i !=0){
                LiteralToImplicationClause.put(i, new ArrayList<Integer>());
            }
       }

       if( verbosity>0){
            for (int i = 0; i < formula.getClauses().size(); i++) {
                    System.out.println("CLAUSE: "+ i + " ===  " + formula.getClauses().get(i));
            }
        }

    }

    public Solver( Reader input_reader) throws Exception{
        rand = new Random(seed);

        formula = Formula_IO.ReadFormula(input_reader);
        twoWatch = new TwoWatch(formula);

        for (int i = -formula.getNumVariables(); i <= formula.getNumVariables(); i++) {
            if (i !=0){
                LiteralToImplicationClause.put(i, new ArrayList<Integer>());
            }
       }

     
    }




    public String Solve(){

        

        if (verbosity>0) System.out.println("INITIAL UNITS: " + formula.getInitialUnits().toString());

        UnitPropogate(formula.getInitialUnits());


        




        do {
            
            // whilst there a conflict keep on backjumping units and reversing decisions

            if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT BEFORE: "+ partialAssignment.toString());
            
            while( conflictClause != null ){
                
                if (verbosity>0) System.out.println("ASSIGNMENT CONFLICTING");

                if( decisionStack.size() == 0) return UNSATISFIABLE;


                // analyse conflict 

                // reverse partial assignment by looking at its initial size in the decision stack 
                // Integer lastDecision = decisionStack.pop();
                // partialAssignment = partialAssignment.subList(0, lastDecision);

                // if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER TRIM: " + partialAssignment.toString());

                // DecisionToLevel.remove(lastDecision);
                // decisionLevel -= 1;
                

                // add learned clause to formula 

                // propagate new units made after
                
                // adding reverse of decision FOR NOW, and REMOVING decision HERE  
                // UnitPropogate(Arrays.asList(   -partialAssignment.remove( partialAssignment.size()-1 )  )); 

                AnalyseConflict();

                if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER UP: " + partialAssignment.toString());


            }

            if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER: "+ partialAssignment.toString());


            if (partialAssignment.size() != formula.getNumVariables()){

                // make decision on unassigned variables
                Integer decision = Decide();

                if (verbosity>0) System.out.println("DECISION MADE: " + decision);


                // increase decision level 
                decisionLevel += 1;
                
                

        
                LiteralToImplicationClause.put(decision, new ArrayList<Integer>());

                // add to partial assignment
                UnitPropogate(Arrays.asList(decision));
                
                // record last index of partial assignment after making decision and unit propgating 
                DecisionToLevel.put(decision, partialAssignment.size()-1);

                // record decision made in decision stack 
                decisionStack.add(decision);

                
                
                if (verbosity>0) System.out.println("PARTIAL ASSIGNMENT AFTER DECISION AND UP: " + partialAssignment.toString());
            
            }


        } while ( partialAssignment.size() != formula.getNumVariables() | conflictClause!=null );

        
        if (verbosity>0) System.out.println("FINAL ASSIGNMENT: " + partialAssignment.toString());

        return SATISFIABLE;
    }


    private int Decide(){

        int random_literal;

        do{ 

            random_literal = rand.nextInt(-formula.getNumVariables(), formula.getNumVariables()+1 ) ;

        }
        while (partialAssignment.contains(random_literal) | partialAssignment.contains(-random_literal) | random_literal == 0 );
        
        return random_literal;
    }



    /**
     * @param initial_units , units to propogate
     * @return returns true if a conlfict has occurred, false otherwise, also updates partial assignment 
     */
    private void UnitPropogate(List<Integer> initial_units){



        List<Integer> units = new ArrayList<Integer>(initial_units);

        
        
        while ( !units.isEmpty() ){

            


            Integer unit_to_add = units.remove(0);
            if (verbosity>0) System.out.println("UNIT ADDED : " + unit_to_add);

            if (verbosity>0) System.out.println("UNITS LEFT NO CONFLICT YET : " + units.toString());

            if (!partialAssignment.contains(unit_to_add) && !partialAssignment.contains(-unit_to_add)){
               
          
              

                partialAssignment.add(unit_to_add);
                conflictClause = twoWatch.UpdateWatchedLiterals(unit_to_add, partialAssignment, units, LiteralToImplicationClause);


            }
            else{
                System.out.println("CLEARING IMPLICATIONS FOR PREASSIGNED LITERAL: " + unit_to_add);
                if (partialAssignment.contains(unit_to_add)) LiteralToImplicationClause.get(unit_to_add).clear();
                if(partialAssignment.contains(-unit_to_add))LiteralToImplicationClause.get(-unit_to_add).clear();
            }

        



            if ( conflictClause != null){

                // clear units that have implications associated with them 
                // but have not been added to the partial assignment yet 
                if (verbosity>0) System.out.println("UNITS LEFT: " + units.toString());
                for (Integer unit : units) {
                    LiteralToImplicationClause.get(unit).clear();
                }

                return;
            }





            


        }

        


    }




    private void AnalyseConflict(){

        Integer lastDecision = decisionStack.peek();
        Integer lastDecisionLevel  = DecisionToLevel.get( lastDecision );
        Integer secondLastDecision = null;

        
        Stack<Integer> clauses = new Stack<>();
        clauses.push(conflictClause);

        HashSet<Integer> learnedClause = new HashSet<>();


        int c =0;
        if (verbosity>0) System.out.println("DECISION STACK: " + decisionStack.toString() + " \n" );
        if (verbosity>0) System.out.println("DECISION TO PARTIAL ASSIGNMENT SIZE: " + DecisionToLevel.toString());
        if (verbosity>0) System.out.println("LITERAL TO IMPLICATIONS: " + LiteralToImplicationClause.toString());


        while ( !clauses.isEmpty()){
            if (verbosity>0) System.out.println("\n ======================================================= \n ");
            if (verbosity>0) System.out.println("CLAUSES LEFT: " + clauses.toString());
            Integer current_clause = clauses.pop();
            if (verbosity>0) System.out.println("ANALYSING CLAUSE: " + formula.getClauses().get(current_clause));
            c+=1;
            // if (verbosity>0) if (c>50){System.exit(0);}

            for (Integer literal : formula.getClauses().get(current_clause) ) {
                
                if (verbosity>0) System.out.println("LITERAL IN CLAUSE: " + literal);
                // if not a decision literal, just add the clause that inferred this literal
                if ( !DecisionToLevel.containsKey(-literal) ){


                    if (verbosity>0) System.out.println("NOT A DECISION");

                    // if the negation of the literal is in the partial assignment then we know it contributed
                    // to the clause 
                    if( partialAssignment.contains(-literal) ){
                        if (verbosity>0) System.out.println("NOT AN INFERRED UNIT");
                        clauses.addAll( LiteralToImplicationClause.get( -literal ) );
                        if (verbosity>0) System.out.println("CLAUSES ADDED: " + LiteralToImplicationClause.get( -literal ));
                    }
                    continue;

                }


                // if this literal is a decision literal 
                learnedClause.add( literal );

                // check if this is possibly the second to last decision literal 
                if ( secondLastDecision == null &&  DecisionToLevel.get(-literal) < lastDecisionLevel ){
                    secondLastDecision = -literal;
                    if (verbosity>0) System.out.println("NEW SECOND LAST DECISION CANDIDATE: "+ secondLastDecision);
                    
                }
                else if (   secondLastDecision!=null &&
                            DecisionToLevel.get(-literal) > DecisionToLevel.get(secondLastDecision) &&
                            DecisionToLevel.get(-literal) < lastDecisionLevel
                        ){
                    secondLastDecision = -literal;
                    if (verbosity>0) System.out.println("NEW SECOND LAST DECISION CANDIDATE: "+ secondLastDecision);

                }

                // if( secondLastDecision!=0 && DecisionToLevel.get(-literal) > DecisionToLevel.get(secondLastDecision)  && DecisionToLevel.get(-literal) < lastDecisionLevel  ){
                //     secondLastDecision = -literal;
                // }
                

            }

        }




        // reverse decisions 
        ReverseDecisions( secondLastDecision);

        // add learned clause 
        AddLearnedClause(learnedClause, -lastDecision);


        // unit propgate with reverse of last decision 
        UnitPropogate(Arrays.asList(-lastDecision));

        
    }


    private void ReverseDecisions( Integer second_last_decision){

        if (verbosity>0) System.out.println("SECOND LAST DECISION: " + second_last_decision);

        // there is no second to last decision, and we are reversing the first decision
        if(second_last_decision == null ){
            for (int i = partialAssignment.size()-1 ; i > formula.getInitialUnits().size()-1 ; i--) {
                
                // clear all inferences for this literal 
                if (verbosity>0) System.out.println("LAST ASSISNMENT: " + partialAssignment.getLast());
                LiteralToImplicationClause.get(partialAssignment.removeLast()).clear();
            }

            decisionStack.clear();
            DecisionToLevel.clear();

            

            return;
        }


        
        for (int i = partialAssignment.size()-1 ; i > DecisionToLevel.get(second_last_decision); i--) {
            
            // clear all inferences for this literal 
            LiteralToImplicationClause.get(  partialAssignment.removeLast() ).clear();
            
        }

        // remove decisions from decision stack 
        while( decisionStack.peek() != second_last_decision){
            // remove decision from decision to decision level map
            DecisionToLevel.remove(decisionStack.pop());
        }






    }


    private void AddLearnedClause(HashSet<Integer> learned_clause, Integer last_decision){
        if (verbosity>0) System.out.println("LEARNED CLAUSE: " + learned_clause.toString() );

        if (verbosity>0) System.out.println("NEW UNIT: "+  last_decision);

        // add to formula 
        Integer new_clause_index = formula.AddClause(learned_clause);

        // add to two watch 
        twoWatch.AddLearnedClause(new_clause_index,last_decision);


        // add to inference mapping
        LiteralToImplicationClause.get(last_decision ).clear(); 
        LiteralToImplicationClause.get(last_decision ).add( new_clause_index );

    }

    // private void addDecision(Integer decision_to_add){


    //     partialAssignment.add(decision_to_add);


    //     UnitToDecisions.put(decision_to_add, Arrays.asList(decision_to_add));


    //     List<Integer> newUnits = new_units

    //     twoWatch.UpdateWatchedLiterals(decision_to_add, partialAssignment, partialAssignment, partialAssignment)
    // }



    



    public static void main( String[] args) throws Exception{

        verbosity = 1;
  
        // for (int i = 0; i < 10000; i++) {
            
            seed=0;
            if (verbosity>0) System.out.println("SEED : " + seed );

        // Solver solver = new Solver("/home/waqee/CDCL2/cdcl/dpll_tests/medium/prop_rnd_17596_v_11_c_46_vic_3_3.cnf");
        Solver solver = new Solver("/home/waqee/CDCL2/cdcl/dpll_tests/hard/prop_rnd_401228_v_82_c_348_vic_3_3.cnf");

        // Solver solver = new Solver(new InputStreamReader(System.in));

        System.out.println(solver.Solve());
        // }

    } 
  


}




