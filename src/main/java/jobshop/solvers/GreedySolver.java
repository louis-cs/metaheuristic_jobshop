package jobshop.solvers;

import jobshop.Priority;
import jobshop.Instance;
import jobshop.Result;
import jobshop.Solver;
import jobshop.encodings.ResourceOrder;
import jobshop.encodings.Task;
public class GreedySolver implements Solver {

    private Priority priority ;
    private Instance instance;
    private int[] realisable ;


    private int[] endJobs ;
    private int[] releaseTimeOfMachine ;
    private int EST = 0;

    public GreedySolver(Priority priority) {
        this.priority = priority ;

    }

    @Override
    public Result solve(Instance instance, long deadline) {
        this.instance = instance;
        realisable = new int[instance.numJobs] ;
        endJobs = new int[instance.numJobs] ;
        releaseTimeOfMachine = new int[instance.numMachines] ;
        ResourceOrder solution = new ResourceOrder(instance) ;

        boolean hasRealisable = true ;
        Task taskPrio;

        while (hasRealisable) {

            //New empty task
            taskPrio = new Task(0, 0) ;

            //Here we select the proper task depending on the selected priority
            switch (this.priority) {
                case EST_SPT :
                    Task[] selectedTask = getMinimalESTTasks() ;
                    int EST_SPT = Integer.MAX_VALUE ;
                    for(Task currentTask : selectedTask){
                        if (currentTask != null) {
                            if(instance.duration(currentTask) < EST_SPT){
                                EST_SPT = instance.duration(currentTask);

                                taskPrio = currentTask ;

                            }
                        }
                    }
                    break;
                case EST_LRPT:
                    Task[] selectedTasks = getMinimalESTTasks() ;
                    int EST_LRPT = 0 ;
                    for (Task currentTask : selectedTasks) {
                        if (currentTask != null) {
                            int jobDuration = 0;
                            for(int t = currentTask.task; t<instance.numTasks; t++) {
                                jobDuration += instance.duration(currentTask.job, t) ;
                            }
                            if (jobDuration > EST_LRPT) {
                                EST_LRPT = jobDuration;
                                taskPrio = currentTask ;
                            }
                        }
                    }
                    break;
                case SPT :
                    //Initialize the SPT at a maximum value
                    int SPT = Integer.MAX_VALUE ;
                    //iterate on all the doable tasks
                    for(int i = 0;i<instance.numJobs;i++){
                        if (realisable[i] != instance.numTasks) {
                            //If the duration of the current doable task is lesser
                            // than the actual SPT, actualize it and
                            if(instance.duration(i, realisable[i]) < SPT){
                                SPT = instance.duration(i, realisable[i]);
                                //set the prioritized task with the new values
                                taskPrio = new Task(i, realisable[i]) ;
                            }
                        }
                    }
                    break;
                case LRPT:
                    //Initialize the LRPT at minimum value
                    int LRPT = 0 ;
                    //iterate on all the jobs
                    for (int i = 0; i<instance.numJobs; i++) {
                        int jobDuration = 0;
                        //Compute the remaining time for the current job
                        for(int j = realisable[i]; j<instance.numTasks; j++) {
                            jobDuration += instance.duration(i, j) ;
                        }
                        //If it's greater than the current LRPT, update it
                        if (jobDuration > LRPT) {
                            LRPT = jobDuration;
                            //set the prioritized task with the new values
                            taskPrio = new Task(i, realisable[i]) ;
                        }
                    }
                    break;
            }
            //Once the correct task has been taken out, we get the machine it is supposed to run on...
            int machine = instance.machine(taskPrio) ;

            //... and we update our solution
            solution.tasksByMachine[machine][solution.nextFreeSlot[machine]] = taskPrio;
            solution.nextFreeSlot[machine]++;
            endJobs[taskPrio.job] = instance.duration(taskPrio) + EST ;
            releaseTimeOfMachine[machine] = endJobs[taskPrio.job] ;

            //We still have to update realisable[]
            realisable[taskPrio.job]++;

            //We finally have to check whether there are tasks left in our instance.
            // If not, we get out of the loop.
            hasRealisable = false;
            for (int i = 0; i<instance.numJobs; i++) {
                if (realisable[i] != instance.numTasks) {
                    hasRealisable = true;
                    break;
                }
            }
        }

        return new Result(instance, solution.toSchedule(), Result.ExitCause.Blocked);
    }

    private Task[] getMinimalESTTasks() {
        //EST computing
        int minEST = Integer.MAX_VALUE ;

        //We iterate on each job
        for (int j = 0; j<instance.numJobs; j++) {
            if (realisable[j] != instance.numTasks) {
                //We get the start time for our current task
                int currentST = Math.max(endJobs[j], releaseTimeOfMachine[instance.machine(j, realisable[j])]) ;
                //If found start time is lesser than the one previously found in minEST, we update the minEST value
                if (currentST <= minEST) {
                    minEST = currentST ;
                }
            }
        }
        EST = minEST ;

        //This list will contain every task within the EST range
        Task[] tasksEST = new Task[instance.numJobs] ;
        //Once again iterate on each job
        for (int j = 0; j<instance.numJobs; j++) {
            if (realisable[j] != instance.numTasks) {
                int currentEST = Math.max(endJobs[j], releaseTimeOfMachine[instance.machine(j, realisable[j])]) ;
                //Select only the tasks having the minimal EST
                if (currentEST == minEST) {
                    tasksEST[j] = new Task(j, realisable[j]);
                }
            }
        }
        //Returns a list containing every doable tasks with a minimal EST
        return tasksEST;
    }
}
