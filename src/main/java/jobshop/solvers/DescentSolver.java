package jobshop.solvers;

import jobshop.Instance;
import jobshop.Priority;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DescentSolver implements Solver {

    /** A block represents a subsequence of the critical path such that all tasks in it execute on the same machine.
     * This class identifies a block in a ResourceOrder representation.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The block with : machine = 1, firstTask= 0 and lastTask = 1
     * Represent the task sequence : [(0,2) (2,1)]
     *
     * */
    static class Block {
        /** machine on which the block is identified */
        final int machine;
        /** index of the first task of the block */
        final int firstTask;
        /** index of the last task of the block */
        final int lastTask;

        Block(int machine, int firstTask, int lastTask) {
            this.machine = machine;
            this.firstTask = firstTask;
            this.lastTask = lastTask;
        }
    }

    /**
     * Represents a swap of two tasks on the same machine in a ResourceOrder encoding.
     *
     * Consider the solution in ResourceOrder representation
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (0,2) (2,1) (1,1)
     * machine 2 : ...
     *
     * The swam with : machine = 1, t1= 0 and t2 = 1
     * Represent inversion of the two tasks : (0,2) and (2,1)
     * Applying this swap on the above resource order should result in the following one :
     * machine 0 : (0,1) (1,2) (2,2)
     * machine 1 : (2,1) (0,2) (1,1)
     * machine 2 : ...
     */
    static class Swap {
        // machine on which to perform the swap
        final int machine;
        // index of one task to be swapped
        final int t1;
        // index of the other task to be swapped
        final int t2;

        Swap(int machine, int t1, int t2) {
            this.machine = machine;
            this.t1 = t1;
            this.t2 = t2;
        }

        /** Apply this swap on the given resource order, transforming it into a new solution. */
        void applyOn(ResourceOrder order) {
            //We first get the tasks associated to our indexes
            Task task1 = order.tasksByMachine[machine][t1];
            Task task2 = order.tasksByMachine[machine][t2];

            //And then we effectively swap the values in our tasksByMachine Array for the solver
            order.tasksByMachine[machine][t1] = task2;
            order.tasksByMachine[machine][t2] = task1;
        }
    }

    private Priority priority;

    //Constructor. Same definition and arguments as the Greedy Solver~
    public DescentSolver(Priority p) {
        priority = p;
    }

    @Override
    public Result solve(Instance instance, long deadline) {
        //Setup the reference solver
        Solver solver = new GreedySolver(priority);

        ResourceOrder solution = null;
        int bestMakeSpan = Integer.MAX_VALUE;

        //Setup the resource order associated to our reference solver
        ResourceOrder bestNeighborSolution = new ResourceOrder(solver.solve(instance, -1).schedule);
        //And get its makespan
        int bestNeighborMakeSpan = bestNeighborSolution.toSchedule().makespan();

        //Loop conditions
        while(bestNeighborMakeSpan < bestMakeSpan && System.currentTimeMillis() < deadline) {
            solution = bestNeighborSolution;
            bestMakeSpan = bestNeighborMakeSpan;

            List<Block> blocks = blocksOfCriticalPath(solution);
            // At each iteration, we iterate on the blocks of the critical path of the solution ...
            for (Block currentBlock : blocks) {
                List<Swap> blockNeighbors = neighbors(currentBlock);

                // ... and on each swapped version of the current block.
                for (Swap currentSwap : blockNeighbors) {

                    ResourceOrder currentNeighborSolution = solution.copy();

                    //We apply the swap to our solution
                    currentSwap.applyOn(currentNeighborSolution);
                    int currentNeighborMakeSpan = currentNeighborSolution.toSchedule().makespan();

                    //And check whether it gets better results or not
                    if (currentNeighborMakeSpan < bestNeighborMakeSpan) {
                        bestNeighborSolution = currentNeighborSolution;
                        bestNeighborMakeSpan = currentNeighborMakeSpan;
                    }
                }
            }
        }

        Result.ExitCause exitCause = Result.ExitCause.Blocked;
        if(System.currentTimeMillis() >= deadline) {
            exitCause = Result.ExitCause.Timeout;
        }
        assert solution != null;
        return new Result(instance, solution.toSchedule(), exitCause);

    }

    /** Returns a list of all blocks of the critical path. */
    static List<Block> blocksOfCriticalPath(ResourceOrder order) {
        List<Task> criticalPath = order.toSchedule().criticalPath();
        List<Block> blockList = new ArrayList<>();

        // Temporary list from which we will get the first and last task of each block.
        List<Task> tempList = new ArrayList<>();
        List<Task> orderMachine;
        int currentBlockMachine = -1;
        int taskCount = 0;

        // We iterate through all the tasks in the critical path
        for (Task current : criticalPath) {
            int currentMachine = order.instance.machine(current);

            //The current machine is different from the previous one, so we have to end the current block
            // if it contains more than one task
            if (currentMachine != currentBlockMachine) {

                //If the current block contains more than one single task, we add the block to our list
                if (taskCount > 1) {
                    orderMachine = Arrays.asList(order.tasksByMachine[currentBlockMachine]);
                    Task firstTask = tempList.get(0);
                    Task lastTask = tempList.get(tempList.size() - 1);
                    blockList.add(new Block(currentBlockMachine,
                            orderMachine.indexOf(firstTask),
                            orderMachine.indexOf(lastTask)));
                }

                //We set up the variables for the next block
                currentBlockMachine = currentMachine;
                taskCount = 0;

                //We then remove every task from the temporary list
                if (tempList.size() > 0) {
                    tempList.subList(0, tempList.size()).clear();
                }
                tempList.add(current);
                taskCount+=1;
            }
            else {
                //We add the machine to our temporary list
                tempList.add(current);
                taskCount += 1;
            }
        }

        //There may be a last block we couldn't finish with the list iteration, so if it contains more than one element,
        // we add it to our block list.
        if (taskCount > 1) {
            orderMachine = Arrays.asList(order.tasksByMachine[currentBlockMachine]);
            Task firstTask = tempList.get(0);
            Task lastTask = tempList.get(tempList.size() - 1);
            blockList.add(new Block(currentBlockMachine, orderMachine.indexOf(firstTask),orderMachine.indexOf(lastTask)));
        }

        return blockList;
    }

    /** For a given block, return the possible swaps for the Nowicki and Smutnicki neighborhood */
    static List<Swap> neighbors(Block block) {
        List<Swap> neighborsList = new ArrayList<>();
        // + 1 because I started the task count on 1 hehe, my mistake
        int blockSize = block.lastTask - block.firstTask + 1;

        //If the block size is only two tasks, we only have one swap possibility : swap the first and the last Task
        if (blockSize == 2) {
            neighborsList.add(new Swap(block.machine, block.firstTask, block.lastTask));
        }

        else {
            neighborsList.add(new Swap(block.machine, block.firstTask, block.firstTask + 1));
            neighborsList.add(new Swap(block.machine, block.lastTask - 1, block.lastTask));
        }
        return neighborsList;
    }

}
