fun optimizePairwise(input: Input, initial: Assignment = input.randomAssignment()): Schedule {
    val machines = Array(input.m) { Machine() }
    for (i in 0 until input.n) machines[initial[i]].jobs += i
    for (m in machines) m.time = m.jobs.sumOf { input.t[it] }
    machines.sort()

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

            if (t.size > 32) break@outer  // ugly fail-safe against rare illegal input to m=2 solver
            val (assignment, makespan) = solve2Unsorted(t)

            if (makespan < m0.time) {
                val new0 = ArrayList<Int>(t.size * 3/4)  // hopefully large enough s.t. no resizing is necessary
                val new1 = ArrayList<Int>(new0.size)

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

    val assignment = IntArray(input.n)
    for (j in machines.indices)
        for (i in machines[j].jobs)
            assignment[i] = j

    return Pair(assignment, machines.last().time)
}

private class Machine : Comparable<Machine> {
    var jobs = mutableListOf<Int>()
    var time = 0L
    override fun compareTo(other: Machine) = time.compareTo(other.time)
}
