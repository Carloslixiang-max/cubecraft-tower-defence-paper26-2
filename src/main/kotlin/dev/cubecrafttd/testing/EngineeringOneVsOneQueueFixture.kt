package dev.cubecrafttd.testing

import dev.cubecrafttd.match.*
import java.util.UUID

object EngineeringOneVsOneQueueFixture {
    fun run(): List<FixtureResult> {
        val a=UUID.fromString(
            "00000000-0000-0000-0000-000000053001"
        )
        val b=UUID.fromString(
            "00000000-0000-0000-0000-000000053002"
        )
        val c=UUID.fromString(
            "00000000-0000-0000-0000-000000053003"
        )

        val duplicateQueue=
            EngineeringOneVsOneQueueState()
        val aFirst=
            duplicateQueue.join(a)
        val aAgain=
            duplicateQueue.join(a)

        val busyQueue=
            EngineeringOneVsOneQueueState()
        busyQueue.join(a)
        busyQueue.join(b)
        val busyPair=
            busyQueue
                .pairIfArenaAvailable(
                    false
                )

        val fifoQueue=
            EngineeringOneVsOneQueueState()
        fifoQueue.join(a)
        fifoQueue.join(b)
        fifoQueue.join(c)
        val pair=
            fifoQueue
                .pairIfArenaAvailable(
                    true
                )

        val leaveQueue=
            EngineeringOneVsOneQueueState()
        leaveQueue.join(a)
        leaveQueue.join(b)
        val left=
            leaveQueue.leave(a)

        val pruneQueue=
            EngineeringOneVsOneQueueState()
        pruneQueue.join(a)
        pruneQueue.join(b)
        pruneQueue.join(c)
        val pruned=
            pruneQueue.retainEligible(
                setOf(a,c)
            )

        val restoreQueue=
            EngineeringOneVsOneQueueState()
        restoreQueue.join(a)
        restoreQueue.join(b)
        restoreQueue.join(c)
        val failedPair=
            restoreQueue
                .pairIfArenaAvailable(
                    true
                )!!
        restoreQueue
            .restorePairToFront(
                failedPair
            )

        return listOf(
            FixtureResult(
                "engineering-1v1-queue-join-idempotent",
                aFirst.added &&
                    aFirst.position==1 &&
                    !aAgain.added &&
                    aAgain.position==1 &&
                    duplicateQueue.size()==1
            ),
            FixtureResult(
                "engineering-1v1-queue-busy-arena-preserves-order",
                busyPair==null &&
                    busyQueue.snapshot()==
                        listOf(a,b)
            ),
            FixtureResult(
                "engineering-1v1-queue-fifo-pairing",
                pair==
                    EngineeringOneVsOneQueuePair(
                        a,b
                    ) &&
                    fifoQueue.snapshot()==
                        listOf(c)
            ),
            FixtureResult(
                "engineering-1v1-queue-leave-waiting",
                left &&
                    leaveQueue.snapshot()==
                        listOf(b)
            ),
            FixtureResult(
                "engineering-1v1-queue-prunes-ineligible",
                pruned==setOf(b) &&
                    pruneQueue.snapshot()==
                        listOf(a,c)
            ),
            FixtureResult(
                "engineering-1v1-queue-failed-start-restores-front",
                restoreQueue.snapshot()==
                    listOf(a,b,c)
            )
        )
    }
}
