package com.cdcl;

import java.util.ArrayList;
import java.util.HashMap;
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
        
        

    }




    public  void UpdateWatchedLiterals(Integer affected_literal ){
        List<Integer> affected_clauses= new ArrayList<Integer>(Literal_To_Clause.get(affected_literal));

        for( Integer clause_index: affected_clauses){

            Integer watch_literal_1 = Clause_To_Literal.get(clause_index)[0];
            Integer watch_literal_2 = Clause_To_Literal.get(clause_index)[1];

            Integer old_watch_literal_1 = watch_literal_1;
            Integer old_watch_literal_2 = watch_literal_2;

            // if the watched literal has just been made false or
            // if the watched literal does not have a value then try to switch it
            if( watch_literal_1==0 || watch_literal_1==affected_literal){
                watch_literal_1 = SwitchWatchLiteral(watch_literal_1, watch_literal_2, Formula.Clauses.get(clause_index));
            }
            if(watch_literal_2==0 || watch_literal_2==affected_literal){
                watch_literal_2 = SwitchWatchLiteral(watch_literal_2, watch_literal_1, Clauses.get(clause_index));
            }


            // if watch literals have changed
            // update data structure
            if( watch_literal_1!= old_watch_literal_1){
                Literal_To_Clause.get(old_watch_literal_1).remove(clause_index);
                Literal_To_Clause.get(watch_literal_1).add(clause_index);
                Clause_To_Literal.get(clause_index)[0] = watch_literal_1;
            }

            if( watch_literal_2!= old_watch_literal_2){
                Literal_To_Clause.get(old_watch_literal_2).remove(clause_index);
                Literal_To_Clause.get(watch_literal_2).add(clause_index);
                Clause_To_Literal.get(clause_index)[1] = watch_literal_2;
            }
        }

    } 

}
