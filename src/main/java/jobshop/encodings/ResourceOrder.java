package jobshop.encodings;

import jobshop.Encoding;
import jobshop.Instance;
import jobshop.Schedule;

import java.util.Arrays;

public class ResourceOrder extends Encoding {

    /** A jobNumber * taskNumber matrix containing (jobNumber,taskNumber) couples with the n-th line representing the n-th machine. */
    public final int[][][] jobs;


    public ResourceOrder(Instance instance) {
        super(instance);

        jobs = new int[instance.numJobs][instance.numTasks][2];
        Arrays.fill(jobs, -1);
    }

    @Override
    public Schedule toSchedule() {
        // time at which each machine is going to be freed
        int[] nextFreeTimeResource = new int[instance.numMachines];

        // for each job, the first task that has not yet been scheduled
        int[] nextTask = new int[instance.numJobs];

        // for each task, its start time
        int[][] startTimes = new int[instance.numJobs][instance.numTasks];

        // compute the earliest start time for every task of every job
        for(int job=0;job<jobs.length;job++) {
            for (int machine = 0; machine < jobs[job].length; machine++) {
                //TODO use this code from JobNumbers to construct a schedule with the resourceorder matrix
//                int task = nextTask[job];
//                int currentmachine = instance.machine(job, task);
//                // earliest start time for this task
//                int est = task == 0 ? 0 : startTimes[job][task - 1] + instance.duration(job, task - 1);
//                est = Math.max(est, nextFreeTimeResource[currentmachine]);
//
//                startTimes[job][task] = est;
//                nextFreeTimeResource[machine] = est + instance.duration(job, task);
//                nextTask[job] = task + 1;
            }
        }

        return new Schedule(instance, startTimes);
    }
}
