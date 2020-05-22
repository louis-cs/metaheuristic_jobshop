package jobshop;

import jobshop.solvers.BasicSolver;
import jobshop.solvers.GreedySolver;

import java.io.IOException;
import java.nio.file.Paths;

public class DebuggingMain {

    public static void main(String[] args) {
        try {
            // load the aaa1 instance
            Instance instance = Instance.fromFile(Paths.get("instances/la40"));

            // construit une solution dans la représentation par
            // numéro de jobs : [0 1 1 0 0 1]
            // Note : cette solution a aussi été vue dans les exercices (section 3.3)
            //        mais on commençait à compter à 1 ce qui donnait [1 2 2 1 1 2]
            /*JobNumbers enc = new JobNumbers(instance);
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 1;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 0;
            enc.jobs[enc.nextToSet++] = 1;*/


            //System.out.println("\nENCODING: " + enc);

            //System.out.println("SCHEDULE: " + sched);
            //System.out.println("VALID: " + sched.isValid());
            //System.out.println("MAKESPAN: " + sched.makespan());

            Result resBasic = new BasicSolver().solve(instance, Long.MAX_VALUE);
            Schedule schedBasic = resBasic.getSchedule();
            System.out.println("Solved using Basic Solver");
            System.out.println("VALID: " + schedBasic.isValid());
            System.out.println("MAKESPAN: " + schedBasic.makespan());

            Result resSPT = new GreedySolver(Priority.SPT).solve(instance, 100000);
            Schedule schedSPT = resSPT.getSchedule();
            System.out.println("Solved using Greedy Solver in SPT");
            System.out.println("VALID: " + schedSPT.isValid());
            System.out.println("MAKESPAN: " + schedSPT.makespan());

            Result resJeanne = new GreedySolver(Priority.EST_SPT).solve(instance, 10000);
            Schedule schedJ = resSPT.getSchedule();
            System.out.println("Solved using Greedy Solver in SPT");
            System.out.println("VALID: " + schedJ.isValid());
            System.out.println("MAKESPAN: " + schedJ.makespan());

            Result resLRPT = new GreedySolver(Priority.LRPT).solve(instance, 100000);
            Schedule schedLRPT = resLRPT.getSchedule();
            System.out.println("Solved using Greedy Solver in LRPT");
            System.out.println("VALID: " + schedLRPT.isValid());
            System.out.println("MAKESPAN: " + schedLRPT.makespan());





        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

    }
}
