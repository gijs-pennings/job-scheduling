import kotlin.random.Random

fun optimizePairwise(input: Input, random: Random = Random.Default) =
    optimizePairwise(input, input.getRandomAssignment(random))

fun optimizePairwise(input: Input, initial: Assignment): Schedule {
 /* assert(initial.belongsTo(input)) */
    val machines = initial.toMachines(input)
    var j0 = machines.lastIndex
    outer@while (j0 > 0) {
        val m0 = machines[j0--]
        val n0 = m0.jobs.size

        for (j1 in 0 until j0) {
            val m1 = machines[j1]
            val n1 = m1.jobs.size

            val t = LongArray(n0 + n1)
            for (i in 0 until n0) t[i   ] = input.t[m0.jobs[i]]
            for (i in 0 until n1) t[i+n0] = input.t[m1.jobs[i]]

            if (t.size > 32) break@outer  // ugly fail-safe against rare illegal input to exact solver
            val (assignment, makespan) = solve2(t)

            if (makespan < m0.time) {
                val new1Size = assignment.countOneBits()
                val new0 = ArrayList<Int>(t.size - new1Size)
                val new1 = ArrayList<Int>(new1Size)

                for (i in 0 until n0)
                    (if ((assignment shr i    and 1) == 0) new0 else new1) += m0.jobs[i]
                for (i in 0 until n1)
                    (if ((assignment shr i+n0 and 1) == 0) new0 else new1) += m1.jobs[i]

                m0.jobs = new0
                m1.jobs = new1
                m0.time = new0.sumOf { input.t[it] }
                m1.time = new1.sumOf { input.t[it] }
                machines.sort()  // fast, since already mostly sorted

                j0 = machines.lastIndex  // restart
                continue@outer
            }
        }
    }
    return machines.toSchedule(input)
}
