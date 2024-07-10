package com.cdcl;


import java.io.FileReader;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.stream.IntStream;
import java.util.Random;




public class Solver {

    private static int verbosity = 0;
    private static int seed = new Random().nextInt(); 
    private static Random rand;

    final static String SATISFIABLE = "SATISFIABLE";
    final static String UNSATISFIABLE = "UNSATISFIABLE";
    
    private Formula formula;

    private List<Integer> partialAssignment = new ArrayList<Integer>();
    private HashSet<Integer> partialAssignmentSet = new HashSet<>();
    private int sizeOfPartialAssignmentWithoutAnyDecisions = 0;


    private TwoWatch twoWatch;

    
    
    private Integer conflictClause = null;
    private int numberOfConflicts = 0; 
    


    private HashMap<Integer, List<Integer> > LiteralToImplicationClause = new HashMap<>(); 

    private HashMap<Integer,Integer> DecisionToLevel = new HashMap<Integer,Integer>();

    private Stack<Integer> decisionStack = new Stack<>();

    private PriorityQueue<Integer> vsidsPriorityQueue = new PriorityQueue<>();
    private HashMap<Integer,Float> vsidsScores = new HashMap<>();

    private static int vsidsDecayInterval = 1;
    private static float vsidsBumpRate= 1.2f;
    private static float vsidsDecayRate = 0.95f;

    private int restartCounter = 0;
    
    
    private static int numberOfAllowedLearntClauses = 1000; 

  
    

    public Solver( Formula formula){
        this.formula = formula;

        rand = new Random(seed);

        twoWatch = new TwoWatch(formula);

        for (int i = -formula.getNumVariables(); i <= formula.getNumVariables(); i++) {
            if (i !=0){
                LiteralToImplicationClause.put(i, new ArrayList<Integer>());
            }
       }

       initialiseVSIDS();

    }


    private static List<Integer> getNextSolution(Formula formula, List<Integer> current_solution){


        if (current_solution==null) return null;

        // add inverse of solution to clause list 
        HashSet<Integer> inverse = new HashSet<>();
        for (Integer literal : current_solution) {
            inverse.add(-literal);
        }


        formula.AddClause(inverse);

        Solver solver = new Solver(formula);

        return solver.Solve();
        
    }



    public static List<List<Integer>> getNSolutions(Formula formula, int N){
        
        Solver solver = new Solver(formula);

        List< List<Integer>> solutions = new ArrayList<>();
        List<Integer> current_solution = solver.Solve();


        while(current_solution!=null && solutions.size()< N){

            solutions.add(current_solution);
            current_solution = getNextSolution(formula, current_solution);

        }


        return solutions;
        
        
    }


    public static List<List<Integer>> getAllSolutions(Formula formula){
        
        Solver solver = new Solver(formula);

        List< List<Integer>> solutions = new ArrayList<>();
        List<Integer> current_solution = solver.Solve();


        while(current_solution!=null ){

            solutions.add(current_solution);

            current_solution = getNextSolution(formula, current_solution);

        }


        return solutions;
    }

    
    public static boolean getSatisfiable(Formula formula){

        Solver solver = new Solver(formula);

        return solver.Solve()!=null;
    }

   

  

    


    public List<Integer> Solve(){

        
        // propogate initial units 
        for (Integer initial_unit : formula.getInitialUnits()) {
            UnitPropogate(initial_unit);
            if (conflictClause != null) return null;
        }
        sizeOfPartialAssignmentWithoutAnyDecisions = partialAssignment.size();

        

        do {

            // whilst there a conflict keep on backjumping units and reversing decisions
            while( conflictClause != null ){
                

                if( decisionStack.size() == 0) return null;
              
                AnalyseConflict();
            }

            
            decayVSIDS();


            // forgets all clauses and restarts 
            // frequency of this follows a geometric progression
            if(Math.pow(2, restartCounter) <= numberOfConflicts){
                Forget();
                Restart();
                restartCounter+=1;
            }


            
            
            // Make Decision
            if (partialAssignment.size() != formula.getNumVariables()){

                Decide();        
            
            }

            
        } while ( partialAssignment.size() != formula.getNumVariables() | conflictClause!=null );

        

        return partialAssignment;
    }

    


    private void Decide(){

        // make decision on unassigned variables
        Integer decision = decideVSIDS();


        LiteralToImplicationClause.get(decision).clear();

        // add to partial assignment
        UnitPropogate(decision);
        
        // record last index of partial assignment after making decision and unit propgating 
        DecisionToLevel.put(decision, partialAssignment.size()-1);

        // record decision made in decision stack 
        decisionStack.add(decision);

        
    }

    // to be implemented later, potentially
    private void Forget(){
        // yet to be implemented fully
        // perhaps requires rethinking how we reference clauses :(

        // at the moment, forget just forgets all clauses 
    

        if(formula.getClauses().size() <= formula.getInitialSize() + numberOfAllowedLearntClauses) return;


    
        for (int i = formula.getInitialSize(); i < formula.getClauses().size(); i++) {
            removeClause(i);
        }

        formula.removeLearntClauses();


     



        
    }

 
    private void removeClause(Integer clause_index){

        // remove from two watch structure
        twoWatch.RemoveClause(clause_index);

    }

   
    private void Restart(){
        // reverse decision to null decision level, or the 0th decision level
        for (int i = partialAssignment.size()-1 ; i > sizeOfPartialAssignmentWithoutAnyDecisions-1 ; i--) {

            Integer literal_to_remove = partialAssignment.removeLast();
            partialAssignmentSet.remove(literal_to_remove);
            // clear all inferences for this literal 
            LiteralToImplicationClause.get(literal_to_remove).clear();

        }

        decisionStack.clear();
        DecisionToLevel.clear();
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
            
            // if not assigned add to partial assignment 
            if (! isAssigned(unit_to_add)   ){

          
              

                partialAssignment.add(unit_to_add);
                partialAssignmentSet.add(unit_to_add);

                conflictClause = twoWatch.UpdateWatchedLiterals(unit_to_add, partialAssignmentSet, units, LiteralToImplicationClause);


            }
            // else{

            //     // if literal is already assigned clear implication for its negation
                
            //     // if (partialAssignmentSet.contains(unit_to_add)){
            //     //     LiteralToImplicationClause.get(unit_to_add).clear();
            //     // }
            //     // else{
            //     //     LiteralToImplicationClause.get(-unit_to_add).clear();
            //     // }


            // }

        



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

        numberOfConflicts+=1;

        Integer lastDecision = decisionStack.peek();
        Integer lastDecisionLevel  = DecisionToLevel.get( lastDecision );
        Integer secondLastDecision = null;

        // add conflict clause to initial set of clauses to check for 
        // responsible decision variables
        Stack<Integer> clauses = new Stack<>();
        clauses.push(conflictClause);

        HashSet<Integer> learnedClause = new HashSet<>();


    

        while ( !clauses.isEmpty()){
            
            Integer current_clause = clauses.pop();
            updateVSIDS(formula.getClauses().get(current_clause));

            for (Integer literal : formula.getClauses().get(current_clause) ) {
                
                // if not a decision literal, just add the clause that inferred this literal
                if ( !DecisionToLevel.containsKey(-literal) ){


                    // if the negation of the literal is in the partial assignment then we know it contributed
                    // to the clause 
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


        //update VSIDS data structures 
        updateVSIDS(learnedClause);

        // reverse decisions 
        ReverseDecisions( secondLastDecision);

        // add learned clause 
        AddLearnedClause(learnedClause, -lastDecision);


        // unit propgate with reverse of last decision 
        UnitPropogate(-lastDecision);

        // record new  size of partial assignment for second to last decision
        if(decisionStack.size()==0){
            LiteralToImplicationClause.get(-lastDecision).clear();
            sizeOfPartialAssignmentWithoutAnyDecisions = partialAssignment.size();
        }
        else{
            DecisionToLevel.put( decisionStack.peek(), partialAssignment.size() -1 ) ;
        }
        
    }


    private void ReverseDecisions( Integer second_last_decision){


        // there is no second to last decision, and we are reversing the first decision
        if(second_last_decision == null ){
            
            Restart();
            

            

            return;
        }


        
        for (int i = partialAssignment.size()-1 ; i > DecisionToLevel.get(second_last_decision); i--) {
            
            // clear all inferences for this literal 
            Integer literal_to_remove = partialAssignment.removeLast();

            partialAssignmentSet.remove(literal_to_remove);
            LiteralToImplicationClause.get( literal_to_remove).clear(); 
            
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

  

    private void initialiseVSIDS(){
        // for every variable set its vsds score as the number of occurences it has 
        for (int i = 1; i <= formula.getNumVariables(); i++) {
            
            float occurences = 0; 

            for (HashSet<Integer> clause : formula.getClauses()) {
                if (clause.contains(i) | clause.contains(-i)) occurences+=1;
            }

            vsidsScores.put(i, occurences);
            vsidsPriorityQueue.add(i);

        }

        
    }

    private void updateVSIDS(HashSet<Integer> clause){
        // after a new conflict has been found, 
        // for every variable in the conflict clause increase its vsids score by 1
        for (Integer literal : clause) {
            literal = Math.abs(literal);

            // vsidsScores.put( literal , vsidsScores.get(literal)+1 );
            vsidsScores.put( literal , vsidsScores.get(literal)*vsidsBumpRate );
            
        }
    }

    private void decayVSIDS(){
        if(numberOfConflicts % vsidsDecayInterval ==0){

            for (int i = 1; i <= formula.getNumVariables(); i++) {
                vsidsScores.put(i, vsidsScores.get(i)/vsidsDecayRate );
            
            }
        }

    }

    private int decideVSIDS(){

        if( rand.nextFloat()<0.05) return decideRandom();

        for (Integer decision : vsidsPriorityQueue) {
            if( !isAssigned(decision) && !isAssigned(-decision)){
                return decision;
            }
        }
  
        return 0;

        
    }

    
    /**
     * @param clause
     * 
     * for eventual use in the Forget step 
     * @return clause activity score based on the sum of the vsids scores of the variables in the clause
     */
    private float getClauseActivityScore(HashSet<Integer> clause){


        float activity_score = 0f;
        for (Integer literal : clause) {
            activity_score += vsidsScores.get( Math.abs(literal) );
        }

        return activity_score;
    }


    public static void setConfig(){

    }

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
  


}




