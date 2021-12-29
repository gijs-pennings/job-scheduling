import java.util.*
import kotlin.math.max

fun solve(input: Input): Schedule {
 /* assert(input.n <= 64)
    assert(input.m in 3..5) */

    val t = input.t.toLongArray()
    val initial = optimizePairwise(input, IntArray(input.n) { it % input.m })  // deterministic
    val perfect = (t.sum() + input.m - 1) / input.m  // ceil

    return if (initial.second <= perfect)
        initial  // rare, but to ensure invariant of `solveRecursively`
    else
        solveRecursively(t, input.m, initial.second, perfect) ?: initial
}

/**
 * Schroeppel & Shamir, 1981 and Korf, 2011
 * @param best makespan of best (i.e. shortest) complete schedule found so far
 * @param maxPrvSum maximum sum of subsets constructed in parent calls (i.e. the weakest link)
 */
private fun solveRecursively(t: LongArray, m: Int, best: Long, maxPrvSum: Long): Schedule? {
 /* assert(maxPrvSum < best) */

    // TODO: is m=2 faster if not treated as a special case? (due to weakest link optimality)
    if (m == 2) {
        val schedule = solve2(t)
        return if (schedule.second >= best)
            null
        else
            Pair(IntArray(t.size) { schedule.first shr it and 1 }, schedule.second)
    }

    val A = PairingHeap(t, 1, (t.size + 1) / 2, true)
    val B = PairingHeap(t, (t.size + 1) / 2, t.size, false)

    val sum = t.sum()

    var assignmentCurrent: Assignment4? = null
    var assignmentRemainder: Assignment? = null
    var upper = best
    var lower = sum - (m-1) * (upper-1)

    val bs = ArrayDeque<Schedule4>()
    outer@while (!A.isEmpty()) {
        val a = A.pop()

        val jobs0: Assignment4 = 1L or a.first
        val s0 = t[0] + a.second  // to prune permutations, the first job is assumed always part of the current set

        val thresholdAdd = lower - s0
        val thresholdRem = upper - s0
        while (!B.isEmpty() && B.peekSum() >= thresholdAdd) bs.addLast(B.pop())
        while (!bs.isEmpty() && bs.peekFirst().second >= thresholdRem) bs.pollFirst()

        // TODO: since b is sorted, is another iteration order worthwhile?
        for (b in bs) {
            val jobs: Assignment4 = jobs0 or b.first
            val s = s0 + b.second

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

    return if (upper == best)
        null
    else
        Pair(merge(assignmentCurrent!!, assignmentRemainder!!), upper)
}

private class PairingHeap(
    t: LongArray,
    fromIndex: Int,
    toIndex: Int,
    ascending: Boolean  // i.e. whether to use min heap (or max heap)
) {

    // TODO: subset generation can be bounded (from above)
    private val half0 = generateSubsets(t, fromIndex, (fromIndex + toIndex) / 2)
    private val half1 = generateSubsets(t, (fromIndex + toIndex) / 2, toIndex)
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

// mostly identical to `generateSubsets2` (bad)
private fun generateSubsets(t: LongArray, fromIndex: Int, toIndex: Int): MutableList<Schedule4> {
    val subsets = ArrayList<Schedule4>(1 shl toIndex - fromIndex)
    subsets += Pair(0, 0)
    for (i in fromIndex until toIndex) {
        for (sIdx in 0 until subsets.size) {
            val s = subsets[sIdx]
            subsets += Pair(1L shl i or s.first, t[i] + s.second)
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
