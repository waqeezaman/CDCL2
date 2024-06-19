package com.cdcl;

import java.util.ArrayList;
import java.util.List;



public class Solver {

    final static String SATISFIABLE = "SATISFIABLE";
    final static String UNSATISFIABLE = "UNSATISFIABLE";
    
    private Formula Formula;


    private static List<Integer> PartialAssignment = new ArrayList<Integer>();



    // two watch literal



    public Solver(String path)throws Exception{


        Formula = Formula_IO.ReadFormula(path); 

        PartialAssignment = new ArrayList<Integer>(Formula.getInitialUnits());

                


        

    }




    public String Solve(){



        return SATISFIABLE;
    }


    private void Decide(){}

    private void UnitPropogate(){}

    



    public static void main( String[] args) throws Exception{

        Solver solver = new Solver("/home/waqee/CDCL/dpll_tests/simple/prop_rnd_962258_v_5_c_21_vic_2_4.cnf");

        // solver.Formula.OutputClauses();

        System.out.println(solver.Solve());

    }



}




