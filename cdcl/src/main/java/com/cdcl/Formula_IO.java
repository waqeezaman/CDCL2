package com.cdcl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Formula_IO {

    public static Formula ReadFormula(Reader input_reader) throws Exception{
            var in = new BufferedReader(input_reader);

            int numvars=0;

            List<HashSet<Integer>> clauses = new ArrayList< HashSet<Integer>>();
            
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                    if (line.charAt(0) =='c'){
                        continue;
                    }
                    if( line.contains("p cnf")){

                        String clauses_vars = line.replace("p cnf","");
                        clauses_vars = clauses_vars.trim();
                        
                        String vars = clauses_vars.split(" ")[0];
                        
                        numvars = Integer.parseInt(vars);

                        break;
                    }
            }

            while (true) {

                String line = in.readLine();

                if (line == null) break;
                
                    String[] vars_in_clause = line.split(" ");
                    // System.out.println(line);
                    HashSet<Integer> clause = new HashSet<Integer>();

                    for(String s: vars_in_clause){
                        if( Integer.parseInt(s)==0){
                            break;
                        }
                        clause.add( Integer.parseInt(s));
                    }
                    clauses.add(clause);   
            }
            in.close();

            return new Formula(clauses, numvars);  
    }
}
