package com.cdcl;

import java.io.FileReader;
import java.io.InputStreamReader;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

import java.util.Random;

import org.apache.commons.cli.*;


public class Solver {

    private static int verbosity = 0;
    private static Integer seed = null; 
    private static Random rand;
    private static String formulaPath;

    public static int restartGeometricRate = 2;
    public static int numberOfAllowedLearntClauses = 1000; 
    public static String decisionFunction = "vsids";

    public static int vsidsDecayInterval = 1;
    public static float vsidsBumpRate= 1.2f;
    public static float vsidsDecayRate = 0.95f;
    public static float vsidsRandomDecideProbability=0.1f;

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

    private int restartCounter = 0;


    public Solver( Formula formula){
        this.formula = formula;

        if(seed!=null){
            rand = new Random(seed);
        }
        else{
            rand = new Random();
        }

        twoWatch = new TwoWatch(formula);

        for (int i = -formula.getNumVariables(); i <= formula.getNumVariables(); i++) {
            if (i !=0){
                LiteralToImplicationClause.put(i, new ArrayList<Integer>());
            }
       }

       initialiseVSIDS();
    }

    /**
     * 
     *  Sets parameters for solver
     *  if argument is left null the default value is used
     * 
     * @param vsids_decay_interval , default: 1
     * @param vsids_bump_rate , defualt: 1.2
     * @param vsids_decay_rate  , default: 0.95
     * @param vsids_random_decide_probability , default: 0.1
     * @param restart_geometric_rate , default: 2
     * @param max_learnt_clauses , default: 1000
     * @param decide_function , either "vsids" or "random", default: vsids
     * @param random_seed , default: null, i.e a random seed
     * @param verbose ,  0 for no output, 1 for some output, default: 0 
     */
    public static void setConfig(Integer vsids_decay_interval,
                                 Float vsids_bump_rate,
                                 Float vsids_decay_rate,
                                 Float vsids_random_decide_probability,
                                 Integer restart_geometric_rate,
                                 Integer max_learnt_clauses,
                                 String decide_function,
                                 Integer random_seed,
                                 Integer verbose
                                ){
        

        if( vsids_decay_interval!=null ) vsidsDecayInterval = vsids_decay_interval;
        if( vsids_bump_rate!=null ) vsidsBumpRate = vsids_bump_rate;
        if( vsids_decay_rate!=null ) vsidsDecayRate = vsids_decay_rate;
        if( vsids_random_decide_probability!=null ) vsidsRandomDecideProbability = vsids_random_decide_probability;


        if( restart_geometric_rate!=null ) restartGeometricRate = restart_geometric_rate;
        if( max_learnt_clauses!=null ) numberOfAllowedLearntClauses = max_learnt_clauses;


        if("random".equals(decide_function)) decisionFunction=decide_function;


        if(random_seed!=null)seed = random_seed;
        if(verbose!=null)verbosity=verbose;
    }

    /**
     * outputs current solver config settings
     */
    public static void outputConfig(){
        System.out.println("\n SOLVER CONFIG \n");
        System.out.println("Decision Function: " + decisionFunction );
        System.out.println("Random Seed: "+ seed );
        System.out.println("Verbosity: "+verbosity );
        System.out.println("Restart Geometric Rate: "+ restartGeometricRate );

        System.out.println("Number of Allowed Learnt Clauses: "+ numberOfAllowedLearntClauses );

        System.out.println("VSIDS Decay Interval: "+ vsidsDecayInterval );
        System.out.println("VSIDS Bump Rate: "+ vsidsBumpRate );
        System.out.println("VSIDS Decay Rate: "+ vsidsDecayRate );
        System.out.println("VSIDS Random Decide Probability: "+ vsidsRandomDecideProbability );
    }
    
    /**
     * inverts the current solution and adds it to the formula, then returns a solution to this new formula
     *  
     * @param formula , current formula
     * @param current_solution, solution that has just been found, we want a new solution unique to this one
     * @return a new solution 
     */
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

    /**
     * @param formula
     * @param N
     * @return first N solutions of the formula
     */
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

    /**
     * @param formula
     * @return all the solutions to the formula
     */
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

    /**
     * @param formula
     * @return true if the formula is satisfiable, false otherwise
     */
    public static boolean getSatisfiable(Formula formula){
        Solver solver = new Solver(formula);
        return solver.Solve()!=null;
    }

    /**
     * runs cdcl algorithm 
     * @return a solution to the formula
     */
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
            if(Math.pow(restartGeometricRate, restartCounter) <= numberOfConflicts){
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

    /**
     * picks a decision variable and adds it to the partial assignment
     */
    private void Decide(){

        // make decision on unassigned variables
        Integer decision;
        if(decisionFunction=="vsids"){
            decision = decideVSIDS();
        }else{
            decision = decideRandom();
        }

        // if a literal has been decided, then it has not been implied by anything
        LiteralToImplicationClause.get(decision).clear();

        // add to partial assignment
        UnitPropogate(decision);
        
        // record last index of partial assignment after making decision and unit propgating 
        DecisionToLevel.put(decision, partialAssignment.size()-1);

        // record decision made in decision stack 
        decisionStack.add(decision);
    }
    
    /**
     * Currently forgets all learnt clauses,
     * yet to be implemented fully
     */
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
 
    /**
     * removes clause from two watch structure
     * @param clause_index , index of clause to remove
     */
    private void removeClause(Integer clause_index){

        // remove from two watch structure
        twoWatch.RemoveClause(clause_index);

    }

    /**
     * reverses partial assignment to the 0th decision level, the level at which no decisions have been taken
     */
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
     * propgates units on to partial assignment 
     * updates the implications of literals, as units are propogated
     * sets conflictClause to index of conflicting clause if a conflict has been found 
     * 
     * @param initial_unit , unit to propogate 
     */
    private void UnitPropogate(int initial_unit){
        List<Integer> units = new ArrayList<Integer>();
        units.add(initial_unit);
        
        
        while ( !units.isEmpty() ){

            Integer unit_to_add = units.remove(0);
            
            if (! isAssigned(unit_to_add)   ){

                partialAssignment.add(unit_to_add);
                partialAssignmentSet.add(unit_to_add);

                conflictClause = twoWatch.UpdateWatchedLiterals(unit_to_add, partialAssignmentSet, units, LiteralToImplicationClause);


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

    /**
     * backtracks after a conflict has been found to a higher decision level
     * @param second_last_decision , second last decision that was responsible for the conflict, null if only one decision was responsible
     */
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

        if( rand.nextFloat()<vsidsRandomDecideProbability) return decideRandom();

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


    private static void displayHelp(Options options){
        System.out.println("OPTIONS: \n \n ");
        for (Option option : options.getOptions()) {
            System.out.println(option.getValueSeparator()+option.getOpt() + " -- " + option.getLongOpt() + " -- " + option.getDescription()+"\n\n");
        }
            
    }

    private static void processArgs(String[] args){
        Options options = new Options();


        // add all options

        options.addRequiredOption("p", "path", true, "REQUIRED: path to cnf file");

        options.addOption("vrdp", "vsids random decision rate probability", true, "The rate at which the VSIDS decide function chooses a random literal, default: 0.1");
        options.addOption("vdr", "vsids decay rate", true, "vsids decay rate default:0.95");
        options.addOption("vdi", "vsids decay interval", true, "After how many conflicts the vsids scores of each variable decay default: 1");
        options.addOption("vbr", "vsids bump rate", true, "The factor by which variables that are found in clauses leading to conflicts using the vsids heuristic, default: 1.2 ");
        
        options.addOption("seed", "seed", true, "random seed");

        options.addOption("rgr", "restart geometric rate", true, "restarts occur every rgr^n conflicts, where n is incremented at every restart, default is 2");
        options.addOption("df", "decision function", true, "decision function used for selecting decision literal, default: vsids");

        options.addOption("lcm", "learnt clauses maximum", true, "maximum number of allowed learnt clauses, default: 1000");

        options.addOption("v", "verbosity", false, "verbosity level, 0 for no output, 1 for some output. defualt: 0");
        options.addOption("h", "help", false, "outputs available options and exits");

                
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        // if there is an error with the options entered, display help menu
        try {
            cmd = parser.parse( options, args);

        } catch (MissingOptionException e) {

            displayHelp(options);
            System.exit(0);
        }
        catch(ParseException p){
            displayHelp(options);
            System.exit(0);
        }


        // display help menu 
        if(cmd.hasOption("h")){
            displayHelp(options);
            System.exit(0);
        }

        
        formulaPath = cmd.getOptionValue("p");


        if(cmd.hasOption("vdi")) vsidsDecayInterval= Integer.valueOf(cmd.getOptionValue("vdi"));
        if(cmd.hasOption("vbr")) vsidsBumpRate= Float.valueOf(cmd.getOptionValue("vbr"));
        if(cmd.hasOption("vdr")) vsidsDecayRate= Float.valueOf(cmd.getOptionValue("vdr"));
        if(cmd.hasOption("vrdp")) vsidsRandomDecideProbability= Float.valueOf(cmd.getOptionValue("vrdp"));

        if(cmd.hasOption("rgr")) restartGeometricRate = Integer.valueOf(cmd.getOptionValue("rgr"));
        if(cmd.hasOption("lcm")) numberOfAllowedLearntClauses = Integer.valueOf(cmd.getOptionValue("lcm"));

        if(cmd.hasOption("df")) decisionFunction = cmd.getOptionValue("df");
        if(cmd.hasOption("seed")) seed = Integer.valueOf(cmd.getOptionValue("seed"));
        if(cmd.hasOption("v")) verbosity = 1;

    }
   
    // // Include this function for testing purposes
    // public static void main( String[] args) throws Exception{
    //     if (verbosity>0) System.out.println("SEED : " + seed );

    //     Formula formula = Formula_IO.ReadFormula(new InputStreamReader(System.in));
   
    //     if (Solver.getSatisfiable(formula)){
    //         System.out.println(SATISFIABLE);
    //     }
    //     else{
    //         System.out.println(UNSATISFIABLE);
    //     }
        
    // } 
     
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

}
