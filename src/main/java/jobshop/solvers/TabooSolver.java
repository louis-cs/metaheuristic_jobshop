package jobshop.solvers;

import java.util.ArrayList;
import java.util.List;

import jobshop.Instance;
import jobshop.Priority;
import jobshop.Result;
import jobshop.Result.ExitCause;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import static jobshop.solvers.DescentSolver.blocksOfCriticalPath;
import static jobshop.solvers.DescentSolver.neighbors;

public class TabooSolver implements Solver {

    private int maxIter;
    private int dureeTaboo;
    private int[][] visited;

    public TabooSolver(int dureeTaboo, int maxIter) {
        this.maxIter = maxIter;
        this.dureeTaboo = dureeTaboo;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        //Once again we get our basis solution from the greedy solver
        GreedySolver greedySolver = new GreedySolver(Priority.EST_LRPT);
        ResourceOrder bestOrder = new ResourceOrder(greedySolver.solve(instance, System.currentTimeMillis() + 10).schedule);
        ResourceOrder currentOrder = bestOrder.copy();
        //this is the list of all visited swaps.
        visited = new int[instance.numJobs * instance.numTasks][instance.numJobs * instance.numTasks];
        int k = 0;
        boolean hasChanged = true;

        while (k<maxIter && (deadline - System.currentTimeMillis() > 1) && hasChanged) {
            //If no better neighbor was found, we get out of the loop.
            hasChanged = false;

            List<DescentSolver.Block> blocks = blocksOfCriticalPath(currentOrder);
            List<DescentSolver.Swap> swaps = new ArrayList<>();
            for (DescentSolver.Block b : blocks) {
                swaps.addAll(neighbors(b));
            }

            ResourceOrder bestNeighbor = new ResourceOrder(instance);
            int bestMakespan = Integer.MAX_VALUE;

            DescentSolver.Swap bestSwap = null;
            //In this loop, we go through all valid neighbors...
            for (DescentSolver.Swap currentSwap : swaps) {
                if (!isTaboo(currentSwap, currentOrder, k)) {
                    ResourceOrder currentNeighbor = currentOrder.copy();
                    currentSwap.applyOn(currentNeighbor);
                    //...and select the best amongst them.
                    if (currentNeighbor.toSchedule().makespan() < bestMakespan) {
                        bestMakespan = currentNeighbor.toSchedule().makespan();
                        bestNeighbor = currentNeighbor.copy();
                        bestSwap = currentSwap;
                        hasChanged = true;
                    }
                }
            }
            //If a better swap has been found, add it to the visited[][] list...
            if (bestSwap != null) {
                addTaboo(bestSwap, currentOrder, k);
            }
            currentOrder = bestNeighbor;
            if (bestMakespan < bestOrder.toSchedule().makespan()) {
                bestOrder = bestNeighbor.copy();
            }
            k++;
        }
        ExitCause exit = (k<maxIter ? ExitCause.Timeout : ExitCause.Blocked);

        return new Result(instance, bestOrder.toSchedule(), exit);
    }

    private void addTaboo(DescentSolver.Swap swap, ResourceOrder order, int k) {
        Task task1 = order.tasksByMachine[swap.machine][swap.t1];
        Task task2 = order.tasksByMachine[swap.machine][swap.t2];
        visited[task2.job * order.instance.numTasks+task2.task][task1.job * order.instance.numTasks + task1.task] = k + dureeTaboo;
    }

    private boolean isTaboo(DescentSolver.Swap swap, ResourceOrder order, int k) {
        Task task1 = order.tasksByMachine[swap.machine][swap.t1];
        Task task2 = order.tasksByMachine[swap.machine][swap.t2];
        return k < visited[task1.job * order.instance.numTasks + task1.task][task2.job * order.instance.numTasks+task2.task];
    }

}

















































//Well this isn't working out. This is a stupidly placed back-up.

/*package jobshop.solvers;

import jobshop.Instance;
import jobshop.Priority;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.List;

public class TabooSolver implements Solver {
    private int dureeTaboo;
    private int maxIter;

    public TabooSolver(int dureeTaboo, int maxIter) {
        this.dureeTaboo = dureeTaboo;
        this.maxIter = maxIter;
    }

    private void addToTaboo(int[][] s_taboo, DescentSolver.Swap swap, ResourceOrder s, int k) {
        Task t1 = s.tasksByMachine[swap.machine][swap.t1];
        Task t2 = s.tasksByMachine[swap.machine][swap.t2];
        s_taboo[t2.job * s.instance.numTasks + t2.task][t1.job * s.instance.numTasks + t1.task] = k + dureeTaboo;
    }

    private boolean isTabooOk(int[][] s_taboo, DescentSolver.Swap swap, ResourceOrder s, int k) {
        Task t1 = s.tasksByMachine[swap.machine][swap.t1];
        Task t2 = s.tasksByMachine[swap.machine][swap.t2];
        return k > s_taboo[t2.job * s.instance.numTasks + t2.task][t1.job * s.instance.numTasks + t1.task];
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        Solver solver = new GreedySolver(Priority.LRPT);
        ResourceOrder s_star = new ResourceOrder(solver.solve(instance, -1).schedule);
        ResourceOrder s = s_star.copy();

        int[][] s_taboo = new int[instance.numJobs * instance.numTasks][instance.numJobs * instance.numTasks];
        int k = 0;

        while(k < maxIter && System.currentTimeMillis() < deadline) {
            k++;

            DescentSolver.Swap best_swap = null ;

            //choisir le meilleur voisin de s' non tabou
            List<DescentSolver.Block> blocks = DescentSolver.blocksOfCriticalPath(s);

            ResourceOrder s_prime = new ResourceOrder(instance);
            int best_makespan = Integer.MAX_VALUE;

            for (DescentSolver.Block block : blocks) {
                for (DescentSolver.Swap swap : DescentSolver.neighbors(block)) {

                    if(isTabooOk(s_taboo, swap, s, k)) {
                        ResourceOrder neighbor = s.copy();
                        swap.applyOn(neighbor);

                        if(neighbor.toSchedule().makespan() < best_makespan) {
                            best_makespan = neighbor.toSchedule().makespan();
                            s_prime = neighbor.copy();
                            best_swap = swap;
                        }
                    }

                }
            }

            if (best_swap != null) {
                addToTaboo(s_taboo, best_swap, s, k);
            }

            //s <- s'
            s = s_prime;

            if(best_makespan < s_star.toSchedule().makespan()) {
                s_star = s_prime.copy();
            }
        }

        Result.ExitCause exitCause = Result.ExitCause.Blocked;
        if(System.currentTimeMillis() >= deadline) {
            exitCause = Result.ExitCause.Timeout;
        }

        return new Result(instance, s_star.toSchedule(), exitCause);
    }
}*/
