package com.cdcl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class TwoWatch {
    

    // do i really need both of these????
    // which stores the literals in the formula and the clauses that they are being watched in 
    private static HashMap<Integer,ArrayList<Integer>> Literal_To_Clause= new HashMap<Integer,ArrayList<Integer>>();

    // which stores clauses and the literals in them that are being watched
    private static HashMap<Integer, int[]> Clause_To_Literal= new HashMap<Integer,int[]>();

    private Formula Formula;




    public TwoWatch(Formula formula){

        Formula = formula;

        // for each literal, initialise list of clauses, where literal is being watched 
        for(int l=1; l<=formula.getNumVariables(); l++){
            Literal_To_Clause.put(l, new ArrayList<>());
            Literal_To_Clause.put(-l, new ArrayList<>());
        }

        for( int c=0; c<formula.getClauses().size(); c++){

            int[] watch_literals = new int[2]  ;
            
            List<Integer> literals_in_clause = new ArrayList<>(formula.getClauses().get(c));

            if (literals_in_clause.size()>=2){
                watch_literals[0] = literals_in_clause.get(0);
                watch_literals[1] = literals_in_clause.get(1);
            }
            else{

                // if clause is a unit clause
                watch_literals[0] = literals_in_clause.get(0);
                watch_literals[1] = literals_in_clause.get(0);

            }

            Clause_To_Literal.put(c,watch_literals);

            Literal_To_Clause.get(watch_literals[0]).add(c);
            Literal_To_Clause.get(watch_literals[1]).add(c);



        }

      
        
        

    }

    




    /**
     * @param added_literal , literal that has just been made true
     * @param partial_assignment 
     * @param new_units , pass in empty list, will be filled with new unit literals that are found, if a conflict occurs, this can be ignored
     * @return  , boolean indicating whether a conflict has occurred
     */
    public boolean UpdateWatchedLiterals(Integer added_literal, List<Integer> partial_assignment, List<Integer> new_units){


        int affected_literal = -added_literal;

        for(int affected_clause: Literal_To_Clause.get(affected_literal)){
            
            int watch_literal_2 = 0;
            
            if (affected_literal == Clause_To_Literal.get(affected_clause)[0]){
                watch_literal_2 =   Clause_To_Literal.get(affected_clause)[1];  
            }
            else if (affected_literal == Clause_To_Literal.get(affected_clause)[1]){
                watch_literal_2 =   Clause_To_Literal.get(affected_clause)[0];  
            }
            

            int potential_new_watched_literal = findUnwatchedLiteral(   affected_literal,
                                                                        watch_literal_2,
                                                                        Formula.getClauses().get(affected_clause),
                                                                        partial_assignment
                                                                    );

            // conflict has occured
            if( potential_new_watched_literal == 0 ){
                new_units.clear();  // cleared to make sure these are not used at all !!
                return true;
            } 
            // new unit literal found 
            else if( potential_new_watched_literal!=-1){
                new_units.add(potential_new_watched_literal);
            }


        }

        // no conflict found 
        return false;


    }




    /**
     * @param watched_literal1
     * @param watched_literal2
     * @param clause
     * @param partial_assignment
     * 
     * Method to reassign watched literals after a literal has been made False
     * 
     * @return returns new unit literal, returns 0 if conflict occurs, returns -1 otherwise
     */
    private int findUnwatchedLiteral(int watched_literal1, int watched_literal2, HashSet<Integer> clause, List<Integer> partial_assignment){


        for(int l: clause){

            if( !partial_assignment.contains(l) && watched_literal1!=l && watched_literal2!=l ){
                
                watched_literal1 = l;
                return -1;

            }

        }

        if( !partial_assignment.contains(watched_literal2) && !partial_assignment.contains(-watched_literal2) ){

            // this is a new unit clause
            return watched_literal2;
        }

        if( partial_assignment.contains(-watched_literal2)){
            // a conflict has occurred
            return 0;
        }

        



        return 0;
    }



    // public  void UpdateWatchedLiterals(Integer affected_literal ){
    //     List<Integer> affected_clauses= new ArrayList<Integer>(Literal_To_Clause.get(affected_literal));

    //     for( Integer clause_index: affected_clauses){

    //         Integer watch_literal_1 = Clause_To_Literal.get(clause_index)[0];
    //         Integer watch_literal_2 = Clause_To_Literal.get(clause_index)[1];

    //         Integer old_watch_literal_1 = watch_literal_1;
    //         Integer old_watch_literal_2 = watch_literal_2;

    //         // if the watched literal has just been made false or
    //         // if the watched literal does not have a value then try to switch it
    //         if( watch_literal_1==0 || watch_literal_1==affected_literal){
    //             watch_literal_1 = SwitchWatchLiteral(watch_literal_1, watch_literal_2, Formula.Clauses.get(clause_index));
    //         }
    //         if(watch_literal_2==0 || watch_literal_2==affected_literal){
    //             watch_literal_2 = SwitchWatchLiteral(watch_literal_2, watch_literal_1, Clauses.get(clause_index));
    //         }


    //         // if watch literals have changed
    //         // update data structure
    //         if( watch_literal_1!= old_watch_literal_1){
    //             Literal_To_Clause.get(old_watch_literal_1).remove(clause_index);
    //             Literal_To_Clause.get(watch_literal_1).add(clause_index);
    //             Clause_To_Literal.get(clause_index)[0] = watch_literal_1;
    //         }

    //         if( watch_literal_2!= old_watch_literal_2){
    //             Literal_To_Clause.get(old_watch_literal_2).remove(clause_index);
    //             Literal_To_Clause.get(watch_literal_2).add(clause_index);
    //             Clause_To_Literal.get(clause_index)[1] = watch_literal_2;
    //         }
    //     }

    // } 

}
