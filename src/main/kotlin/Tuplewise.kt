import kotlin.math.abs

fun optimizeTuplewise(input: Input, k: Int = 4, initial: Assignment = optimizePairwise(input).first): Schedule {
    val combinations = generateCombinations(k - 1, input.m - 1)
    val machines = initial.toMachines(input)
    outer@while (true) {
        for (cPartial in combinations) {
            val c = cPartial + (input.m - 1)
            val cMachines = c.map { machines[it] }
            val cMakespan = cMachines.last().time

            val tIndices = cMachines.map { it.jobs }.concatenateToArray()
            val t = List(tIndices.size) { input.t[tIndices[it]] }

            if (t.size > 64) break  // ugly fail-safe against rare illegal input to exact solver
            val (assignment, makespan) = solve(Input(t, k), cMakespan) ?: continue

            if (makespan < cMakespan) {
                for (m in cMachines) m.jobs.clear()
                for (i in t.indices) cMachines[assignment[i]].jobs += tIndices[i]
                for (m in cMachines) m.time = m.jobs.sumOf { input.t[it] }
                machines.sort()
                continue@outer
            }
        }
        break
    }
    return machines.toSchedule(input)
}

/**
 * Returns a list of all `k`-combinations of the set `{0, 1, .., m-1}`.
 */
fun generateCombinations(k: Int, m: Int): List<IntArray> {
    val combinations = mutableListOf<IntArray>()

    // base case: j = 0
    for (x in 0..m-k) combinations += IntArray(k) { if (it == 0) x else -1 }

    // 'inductive' step: j = 1, .., k-1
    for (j in 1 until k) {
        for (cIdx in combinations.indices) {
            val c = combinations[cIdx]
            for (x in c[j-1]+1 until m-k+j) combinations += c.clone().also { it[j] = x }
            c[j] = m-k+j
        }
    }

    // sort approximately from 'best' to 'worst'
    return combinations
        .map { it to it.sumOf { x -> abs(x - m / 2.0) } }
        .sortedByDescending { it.second }
        .map { it.first }
}
