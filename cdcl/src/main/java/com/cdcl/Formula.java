package com.cdcl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Formula {

        private  List<HashSet<Integer>> Clauses = new ArrayList< HashSet<Integer>>();
        private  int NumVariables;

        private List<Integer> InitialUnits = new ArrayList<>();

        private int InitialSize;


       

        public Formula(List<HashSet<Integer>> clauses, int numvars){

            Clauses = clauses;
            NumVariables = numvars;

            optimise();

            HashSet<Integer> units = new HashSet<>();

            for(HashSet<Integer> clause: Clauses){
                if (clause.size()==1 ){
                   
                    units.addAll(clause);
                    

                }
            }

            InitialUnits.addAll(units);
            InitialSize = Clauses.size();

        }


        private void eliminateTautologies(){

            List<HashSet<Integer>> oldClauses = new ArrayList< HashSet<Integer>>(Clauses);

            for( HashSet<Integer> clause: oldClauses){
                
                if ( isTautology(clause) ){
                    Clauses.remove(clause);
                    
                }
            }

        }


        public static boolean isTautology(HashSet<Integer> clause){

            Iterator<Integer> iterator = clause.iterator();


            while (iterator.hasNext()){

                if ( clause.contains(-iterator.next()) ){
                    return true;
                }

            }

            return false;
    

        }


        public void optimise(){


            eliminateTautologies();;



        }


        public void Remove(int index){
            Clauses.remove(index);
        }

        public List<HashSet<Integer>> getClauses(){
            return Clauses;
        }

        public int getNumVariables(){
            return NumVariables;
        }

        public List<Integer> getInitialUnits(){
            return InitialUnits;
        }

        public int getInitialSize(){
            return InitialSize;
        }

        public void removeLearntClauses(){

            Clauses = Clauses.subList(0, InitialSize);

        }

        /**
         * @param clause , clause to be added to the formula 
         * @return returns the index of the clause added
         */
        public Integer AddClause(HashSet<Integer> clause){


            Clauses.add(clause);

            return Clauses.size()-1;
        }


        public void OutputClauses(){
            System.out.println("CLAUSES");
            for( HashSet<Integer> clause : Clauses){
                System.out.println(clause);
                
            }
            System.out.println("=============================================================================");
        }


        public void OuputToFile(String filepath){

            try {

                FileWriter file = new FileWriter(filepath);

                // write initial information about number of clauses and number of variables
                file.write("c created by me \n");

                file.write("p cnf " + NumVariables + " " + Clauses.size() + "\n");
            


                // write each clause to file
                for (HashSet<Integer> clause : Clauses) {
                    String clause_string = "";
                    for (Integer literal : clause) {
                        clause_string += literal.toString() + " ";
                    }    
                    clause_string += "0\n";

                    file.write(clause_string);
                }


                file.close();


            } catch (IOException e) {
                

                System.out.println("ERROR: File unable to be created");
            }
            


        }
}
