package com.uwc.tooring.scheduling;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.uwc.tooring.TuringService;
import com.uwc.tooring.turing.impl.DefaultTuringMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for unlocking resources of "dead" nodes to make them available.
 */
@Service
public class LocksManager {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private TuringService turingService;

    /**
     * Iterates over all saved tasks (machines) and sets locking flag to false if the nod is unlocked.
     */
    @Scheduled(fixedRate = 10000)
    public void unlockAvailableMachines() {
        if (!turingService.isWorker()) {
            return;
        }
        IMap<String, DefaultTuringMachine> tasksMap = hazelcastInstance.getMap(TuringService.TASKS_MAP);
        for (Map.Entry<String, DefaultTuringMachine> entry : tasksMap.entrySet()) {
            ILock lock = hazelcastInstance.getLock(entry.getKey());
            if (!lock.isLocked()) {
                entry.getValue().setLocked(false);
            }
        }
    }

}
