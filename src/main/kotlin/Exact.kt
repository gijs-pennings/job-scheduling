import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.max
import kotlin.math.min

fun solve(input: Input): Schedule {
    val pairwise = optimizePairwise(input, input.getSimpleAssignment())  // deterministic
    return solve(input, pairwise.second) ?: pairwise
}

/**
 * If [input]`.t` is sorted, this method is generally faster since that allows for more pruning in [generateSubsets].
 * For `input.m == 4` the best results were achieved with an 'interlaced' descending order.
 * @return an optimal solution strictly better than [knownUpper], or `null` if such a solution does not exist
 */
fun solve(input: Input, knownUpper: Long): Schedule? {
 /* assert(input.n <= 64)
    assert(input.m in 3..5)
    assert(input.lowerbound <= knownUpper) */
    if (knownUpper <= input.lowerbound) return null
    return solveRecursively(input.t.toLongArray(), input.m, knownUpper, input.lowerbound)
}

/**
 * Schroeppel & Shamir, 1981 and Korf et al., 2014
 * @param upper0 makespan of best (i.e. shortest) complete schedule found so far
 * @param maxPrvSum maximum sum of subsets constructed in parent calls (i.e. the weakest link)
 */
private fun solveRecursively(t: LongArray, m: Int, upper0: Long, maxPrvSum: Long): Schedule? {
 /* assert(t.isSortedDescending())
    assert(maxPrvSum < best) */

    // TODO: is it more efficient to remove this 'special' case? (since it would allow weakest link optimization)
    if (m == 2) {
        val schedule = solve2(t)
        return if (schedule.second >= upper0)
            null
        else
            Pair(IntArray(t.size) { schedule.first shr it and 1 }, schedule.second)
    }

    val first = t[0]  // to prune permutations, the first job is assumed always part of the current set
    val sum = t.sum()

    var assignmentCurrent: Assignment4? = null
    var assignmentRemainder: Assignment? = null
    var upper = upper0
    var lower = sum - (m-1) * (upper-1)

    val A = PairingHeap(t, upper - first, 1, (t.size + 1) / 2, true)
    val B = PairingHeap(t, upper - first, (t.size + 1) / 2, t.size, false)

    val bs = ArrayDeque<Schedule4>()
    outer@while (!A.isEmpty()) {
        val a = A.pop()

        val jobs0: Assignment4 = 1L or a.first
        val s0 = first + a.second
        if (s0 >= upper) break  // since a.second will only increase

        val thresholdAdd = lower - s0
        val thresholdRem = upper - s0
        while (!B.isEmpty() && B.peekSum() >= thresholdAdd) bs.addLast(B.pop())
        while (!bs.isEmpty() && bs.first().second >= thresholdRem) bs.removeFirst()

        // TODO: is another iteration order more efficient?
        for (b in bs) {
            val jobs: Assignment4 = jobs0 or b.first
            val s = s0 + b.second
            if (s >= upper) continue
            if (s  < lower) break

            val schedule = solveRecursively(t.without(jobs), m-1, upper, max(maxPrvSum, s)) ?: continue
            val makespan = max(s, schedule.second)

            if (makespan < upper) {
                assignmentCurrent = jobs
                assignmentRemainder = schedule.first
                upper = makespan
                if (upper <= maxPrvSum) break@outer  // weakest link optimality
                lower = sum - (m-1) * (upper-1)
            }
        }
    }

    return if (upper == upper0)
        null
    else
        Pair(merge(assignmentCurrent!!, assignmentRemainder!!), upper)
}

private class PairingHeap(
    t: LongArray,
    upper: Long,
    fromIndex: Int,
    toIndex: Int,
    ascending: Boolean  // i.e. whether to use min heap (or max heap)
) {

    private val half0 = generateSubsets(t, upper, fromIndex, (fromIndex + toIndex) / 2)
    private val half1 = generateSubsets(t, upper, (fromIndex + toIndex) / 2, toIndex)
    private val queue: PriorityQueue<Triple<Int, Int, Long>>  // <i0, i1, half0[i0] + half1[i1]>

    init {
        if (ascending) {
            half0.sortBy { it.second }
            half1.sortBy { it.second }
            queue = PriorityQueue(half0.size) { (_, _, x), (_, _, y) -> x.compareTo(y) }
        } else {
            half0.sortByDescending { it.second }
            half1.sortByDescending { it.second }
            queue = PriorityQueue(half0.size) { (_, _, x), (_, _, y) -> y.compareTo(x) }
        }

        // initial combined subsets
        val s1 = half1[0].second
        for (i0 in half0.indices) queue.add(Triple(i0, 0, half0[i0].second + s1))

        // initial pruning
        if (!ascending)
            while (peekSum() >= upper)
                pop()
    }

    fun isEmpty() = queue.isEmpty()

    fun peekSum(): Long {
     /* assert(!isEmpty()) */
        return queue.peek().third
    }

    fun pop(): Schedule4 {
     /* assert(!isEmpty()) */
        val (i0, i1, sum) = queue.poll()
        val i1Next = i1 + 1
        if (i1Next < half1.size) queue.add(Triple(i0, i1Next, half0[i0].second + half1[i1Next].second))
        return Pair(half0[i0].first or half1[i1].first, sum)
    }

}

private fun generateSubsets(t: LongArray, upper: Long, fromIndex: Int, toIndex: Int): MutableList<Schedule4> {
    val subsets = ArrayList<Schedule4>(1 shl toIndex - fromIndex)
    subsets += Pair(0, 0)
    for (i in fromIndex until toIndex) {
        for (sIdx in subsets.indices) {
            val s = subsets[sIdx]
            val x = t[i] + s.second
            if (x < upper) subsets += Pair(1L shl i or s.first, x)
        }
    }
    return subsets
}

private fun LongArray.without(mask: Long): LongArray {
 /* assert((mask and 1L) == 1L) */
    var i = 0
    var m = mask
    return LongArray(size - mask.countOneBits()) {
        do { i++; m = m shr 1 } while ((m and 1L) == 1L)
        this[i]
    }
}

private fun merge(cur: Assignment4, rem: Assignment): Assignment {
    var i = 0
    return IntArray(cur.countOneBits() + rem.size) {
        if ((cur shr it and 1L) == 1L)
            0
        else
            1 + rem[i++]
    }
}
