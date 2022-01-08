import kotlin.math.abs
import kotlin.random.Random

private const val DEFAULT_K = 4

/**
 * Identical to [optimizeTuplewise] (1) but first a good random initial solution is computed.
 */
fun optimizeTuplewise(input: Input, k: Int = DEFAULT_K, random: Random = Random.Default) =
    optimizeTuplewise(input, optimizePairwise(input, random).first, k)

/**
 * Identical to [optimizeTuplewise] (2) but first a good random initial solution is computed.
 */
fun optimizeTuplewise(input: Input, k: Int, triesPerTuple: Int, random: Random = Random.Default) =
    optimizeTuplewise(input, optimizeTuplewise(input, DEFAULT_K, random).first, k, triesPerTuple, random)

/**
 * (1) Attempts to improve an [initial] assignment by optimally solving 'subschedules' of `3 <= `[k]` <= 5` machines
 * until no further progress is possible.
 */
fun optimizeTuplewise(input: Input, initial: Assignment, k: Int = DEFAULT_K): Schedule {
 /* assert(k in 3..5) */
    return optimizeTuplewiseInternal(input, initial, k) { i, ms ->
        if (i.n <= 64)
            solve(i, ms.last().time)?.first
        else
            null  // ugly fail-safe against rare illegal input to exact solver
    }
}

/**
 * (2) Attempts to improve an [initial] assignment by approximately solving 'subschedules' of [k]` >= 6` machines. These
 * subschedules are found by running the 'standard' quadruplewise optimizer [triesPerTuple] times and taking the best.
 * This uses multithreading.
 */
fun optimizeTuplewise(input: Input, initial: Assignment, k: Int, triesPerTuple: Int,
                      random: Random = Random.Default): Schedule {
 /* assert(k >= 6)
    assert(triesPerTuple > 0) */
    return optimizeTuplewiseInternal(input, initial, k) { i, ms ->
        val tries = computeParallel(triesPerTuple) { optimizeTuplewise(i, DEFAULT_K, random) }
        val makespanBestTry = tries.minOf { it.second }
        val makespanCurrent = ms.last().time
        if (makespanBestTry <= makespanCurrent) {
            val bestTries = tries.filter { it.second == makespanBestTry }.map { it.first }
            if (bestTries.size > 1 || makespanBestTry == makespanCurrent) {
                val bestTry = bestTries.minByOrNull { it.calculateScore(i) }!!
                if (makespanBestTry == makespanCurrent && ms.calculateScore(i) <= bestTry.calculateScore(i))
                    // only solution(s) with makespan equal to the current were found, and none were structurally better
                    null
                else
                    // (otherwise)
                    bestTry
            } else
                // improvement(s) were found, and there is a clear 'best' (i.e. with the lowest makespan)
                bestTries.first()
        } else
            // only worse solutions were found
            null
    }
}

private fun optimizeTuplewiseInternal(input: Input, initial: Assignment, k: Int,
                                      optimizeTuple: (Input, List<Machine>) -> Assignment?): Schedule {
 /* assert(initial.belongsTo(input))
    assert(k < input.m) */
    return initial
        .toMachines(input)
        .apply { optimize(input.t, k, getTuplesOrdered(k, input.m), optimizeTuple) }
        .toSchedule(input)
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

/**
 * Returns a list of all [k]-combinations of the set `{0, 1, .., m-1}` in no particular order. Each tuple is sorted in
 * ascending order.
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

private fun Array<Machine>.optimize(tAll: List<Long>, k: Int, tuples: List<IntArray>,
                                    optimizeTuple: (Input, List<Machine>) -> Assignment?) {
    outer@while (true) {
        for (tuple in tuples) {
            val machines = tuple.map { this[it] }
            val tIndices = machines.flatMap { it.jobs }.sortedByDescending { tAll[it] }.interlaced()
            val t = List(tIndices.size) { tAll[tIndices[it]] }

            val assignment = optimizeTuple(Input(t, k), machines) ?: continue

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

private fun Assignment.calculateScore(input: Input): Int {
    val sums = LongArray(input.m)
    for (i in indices) sums[this[i]] += input.t[i]
    return sums.sumOf { (it - input.lowerbound).toInt().squared() }  // smaller is better!
}

private fun List<Machine>.calculateScore(input: Input) = sumOf { (it.time - input.lowerbound).toInt().squared() }
