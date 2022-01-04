import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

fun optimizeTuplewise(input: Input, k: Int = 4, restarts: Int = 0, random: Random = Random.Default): Schedule {
    var machinesBest = optimizePairwise(input, random).first.toMachines(input)  // initial, since pairwise is very fast
    val tuples = getTuplesOrdered(k, input.m)
    machinesBest.optimize(input.t, k, tuples)

    if (restarts > 0) {
        var r = 0
        val restartTuple = getRestartTuple(k, input.m)
        while (r++ < restarts) {
            val machines = machinesBest.deepCopy()
            machines.shuffle(input.t, restartTuple, random)
            machines.optimize(input.t, k, tuples)
            if (machinesBest.last().time > machines.last().time) {
                machinesBest = machines
                r = 0  // restart with restarts :)
            }
        }
    }

    return machinesBest.toSchedule(input)
}

private fun getTuplesOrdered(k: Int, m: Int): List<IntArray> {
    val last = m - 1
    val mid = last / 2.0
    return generateCombinations(k-1, last)
        .map { it + last }
        .map { it to abs(it.sumOf { x -> x - mid }) }
        .sortedBy { it.second }  // approximately from best to worst
        .map { it.first }
}

private fun getRestartTuple(k: Int, m: Int): IntArray {
    val len0 = ceil(1.5 * k).toInt()
    val len = min(m, len0 + len0 % 2)
    return IntArray(len) {
        if (it < (len + 1) / 2)
            it
        else
            m + it - len
    }
}

/**
 * Returns a list of all `k`-combinations of the set `{0, 1, .., m-1}` in no particular order.
 */
private fun generateCombinations(k: Int, m: Int): List<IntArray> {
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

    return combinations
}

private fun Array<Machine>.optimize(tAll: List<Long>, k: Int, tuples: List<IntArray>) {
    outer@while (true) {
        for (tuple in tuples) {
            val machines = tuple.map { this[it] }
            val tIndices = machines.flatMap { it.jobs }.sortedByDescending { tAll[it] }.interlaced()
            val t = List(tIndices.size) { tAll[tIndices[it]] }

            if (tIndices.size > 64) return  // ugly fail-safe against rare illegal input to exact solver
            val (assignment, _) = solve(Input(t, k), machines.last().time) ?: continue

            // if not null, the newly found assignment is better than the current
            for (m in machines) m.jobs.clear()
            for (i in t.indices) machines[assignment[i]].jobs += tIndices[i]
            for (m in machines) m.time = m.jobs.sumOf { tAll[it] }
            sort()

            continue@outer  // restart with first tuple
        }
        break
    }
}

private fun Array<Machine>.shuffle(t: List<Long>, tuple: IntArray, random: Random) {
    val machines = tuple.map { this[it] }
    val jobs = machines.flatMap { it.jobs }
    for (m in machines) m.jobs.clear()
    for (i in jobs) machines.random(random).jobs += i
    for (m in machines) m.time = m.jobs.sumOf { t[it] }
    sort()
}
