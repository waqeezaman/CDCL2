package com.cdcl;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

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
    private HashSet<Integer> partialAssignmentSet = new HashSet<>();


    private TwoWatch twoWatch;

    
    
    private Integer conflictClause = null;

    private HashMap<Integer, List<Integer> > LiteralToImplicationClause = new HashMap<>(); 

    private HashMap<Integer,Integer> DecisionToLevel = new HashMap<Integer,Integer>();

    private Stack<Integer> decisionStack = new Stack<>();

 



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

        

        for (Integer initial_unit : formula.getInitialUnits()) {
            UnitPropogate(initial_unit);
            if (conflictClause != null) return UNSATISFIABLE;
        }
        


        




        do {
            
            // whilst there a conflict keep on backjumping units and reversing decisions

            
            while( conflictClause != null ){
                

                if( decisionStack.size() == 0) return UNSATISFIABLE;


                // analyse conflict 

              

                AnalyseConflict();



            }



            if (partialAssignment.size() != formula.getNumVariables()){

                // make decision on unassigned variables
                Integer decision = Decide();

                LiteralToImplicationClause.get(decision).clear();

                // add to partial assignment
                UnitPropogate(decision);
                
                // record last index of partial assignment after making decision and unit propgating 
                DecisionToLevel.put(decision, partialAssignment.size()-1);

                // record decision made in decision stack 
                decisionStack.add(decision);

                
                
            
            }


        } while ( partialAssignment.size() != formula.getNumVariables() | conflictClause!=null );

        

        return SATISFIABLE;
    }


    private int Decide(){

        return decideRandom();
    }

    private int decideRandom(){
        int random_literal;

        do{ 

            random_literal = rand.nextInt(-formula.getNumVariables(), formula.getNumVariables()+1 ) ;

        }
        while ( isAssigned(random_literal) | random_literal == 0 );

        

        return random_literal;
    }

    private boolean isAssigned(Integer literal){
        return partialAssignmentSet.contains(literal) | partialAssignmentSet.contains(-literal);
    }



    /**
     * @param initial_units , units to propogate
     * @return returns true if a conlfict has occurred, false otherwise, also updates partial assignment 
     */
    private void UnitPropogate(int initial_unit){



        List<Integer> units = new ArrayList<Integer>();
        units.add(initial_unit);
        
        
        while ( !units.isEmpty() ){

            


            Integer unit_to_add = units.remove(0);
            

            // if (!partialAssignment.contains(unit_to_add) && !partialAssignment.contains(-unit_to_add)){
            if (! isAssigned(unit_to_add)   ){

          
              

                partialAssignment.add(unit_to_add);
                partialAssignmentSet.add(unit_to_add);

                conflictClause = twoWatch.UpdateWatchedLiterals(unit_to_add, partialAssignmentSet, units, LiteralToImplicationClause);


            }
            else{


                // if (partialAssignment.contains(unit_to_add)) LiteralToImplicationClause.get(unit_to_add).clear();
                // if(partialAssignment.contains(-unit_to_add))LiteralToImplicationClause.get(-unit_to_add).clear();
                if (partialAssignmentSet.contains(unit_to_add)){
                    LiteralToImplicationClause.get(unit_to_add).clear();
                }
                else{
                    LiteralToImplicationClause.get(-unit_to_add).clear();
                }


            }

        



            if ( conflictClause != null){

                // clear units that have implications associated with them 
                // but have not been added to the partial assignment yet 
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


    

        while ( !clauses.isEmpty()){
            
            Integer current_clause = clauses.pop();
           

            for (Integer literal : formula.getClauses().get(current_clause) ) {
                
                // if not a decision literal, just add the clause that inferred this literal
                if ( !DecisionToLevel.containsKey(-literal) ){


                    // if the negation of the literal is in the partial assignment then we know it contributed
                    // to the clause 
                    // if( partialAssignment.contains(-literal) ){
                    if( partialAssignmentSet.contains(-literal) ){

                        clauses.addAll( LiteralToImplicationClause.get( -literal ) );
                    }
                    continue;

                }


                // if this literal is a decision literal 
                learnedClause.add( literal );

                // check if this is possibly the second to last decision literal 
                if ( secondLastDecision == null &&  DecisionToLevel.get(-literal) < lastDecisionLevel ){
                    secondLastDecision = -literal;
                    
                }
                else if (   secondLastDecision!=null &&
                            DecisionToLevel.get(-literal) > DecisionToLevel.get(secondLastDecision) &&
                            DecisionToLevel.get(-literal) < lastDecisionLevel
                        ){
                    secondLastDecision = -literal;

                }

               
                

            }

        }




        // reverse decisions 
        ReverseDecisions( secondLastDecision);

        // add learned clause 
        AddLearnedClause(learnedClause, -lastDecision);


        // unit propgate with reverse of last decision 
        UnitPropogate(-lastDecision);

        
    }


    private void ReverseDecisions( Integer second_last_decision){


        // there is no second to last decision, and we are reversing the first decision
        if(second_last_decision == null ){
            for (int i = partialAssignment.size()-1 ; i > formula.getInitialUnits().size()-1 ; i--) {
                
                Integer literal_to_remove = partialAssignment.removeLast();
                partialAssignmentSet.remove(literal_to_remove);
                // clear all inferences for this literal 
                LiteralToImplicationClause.get(literal_to_remove).clear();//partialAssignment.removeLast()).clear();

            }

            decisionStack.clear();
            DecisionToLevel.clear();

            

            return;
        }


        
        for (int i = partialAssignment.size()-1 ; i > DecisionToLevel.get(second_last_decision); i--) {
            
            // clear all inferences for this literal 
            Integer literal_to_remove = partialAssignment.removeLast();
            partialAssignmentSet.remove(literal_to_remove);
            LiteralToImplicationClause.get( literal_to_remove).clear(); //partialAssignment.removeLast() ).clear();
            
        }

        // remove decisions from decision stack 
        while( !decisionStack.peek().equals(second_last_decision)){
            
            // remove decision from decision to decision level map
            DecisionToLevel.remove(decisionStack.pop());

        }






    }


    private void AddLearnedClause(HashSet<Integer> learned_clause, Integer last_decision){
        

        // add to formula 
        Integer new_clause_index = formula.AddClause(learned_clause);

        // add to two watch 
        twoWatch.AddLearnedClause(new_clause_index,last_decision);


        // add to inference mapping
        LiteralToImplicationClause.get(last_decision ).clear(); 
        LiteralToImplicationClause.get(last_decision ).add( new_clause_index );

    }

  



    



    public static void main( String[] args) throws Exception{

        verbosity = 0;
  
        
            
        seed=0;
        if (verbosity>0) System.out.println("SEED : " + seed );

        // Solver solver = new Solver("/home/waqee/CDCL2/cdcl/dpll_tests/simple/prop_rnd_945782_v_3_c_12_vic_2_4.cnf");
        // Solver solver = new Solver("/home/waqee/CDCL2/cdcl/dpll_tests/hard/prop_rnd_543508_v_128_c_544_vic_3_3.cnf");

        Solver solver = new Solver(new InputStreamReader(System.in));

        System.out.println(solver.Solve());
        

    } 
  


}




