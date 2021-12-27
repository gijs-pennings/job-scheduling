import java.util.*

fun solve2Unsorted(t: LongArray): Schedule2 {
    val tIndexed = t.withIndex().sortedByDescending { it.value }
    for (i in t.indices) t[i] = tIndexed[i].value  // overwritten!

    val schedule = solve2(t)
    var assignment = 0
    for (i in t.indices)
        if ((schedule.first shr i and 1) == 1)
            assignment = 1 shl tIndexed[i].index or assignment

    return Pair(assignment, schedule.second)
}

// TODO: what's the performance without sorting? and without `computeUpperbound`?
// Horowitz & Sahni, 1974 (with an optimization by Korf & Schreiber, 2013)
fun solve2(t: LongArray): Schedule2 {
    assert(t.size <= 32)
    assert(t.isSortedDescending())

    val sum = t.sum()
    val largest = t[0]
    val perfect0 =  sum      / 2  // floor
    val perfect1 = (sum + 1) / 2  // ceil

    val A = generateSubsets(t, 1, (t.size + 1) / 2)  // with size less than or equal to B's
    val B = generateSubsets(t, (t.size + 1) / 2, t.size)
    A.sortBy { it.second }
    B.sortByDescending { it.second }

    var upper = computeUpperbound(t, sum) + 1  // does not return corresponding assignment, so we need to refind it
    var lower = sum - upper
    var assignment = 0  // uninitialized
    var kA = 0
    var kB = 0
    while (kA < A.size && kB < B.size) {
        val a = A[kA]
        val b = B[kB]
        val x = largest + a.second + b.second  // TODO: can `largest` also be assumed part of the 'other' set?
        if (x <= lower) {
            kA++
        } else if (x < perfect0) {
            upper = sum - x
            lower = x
            assignment = 1 or a.first or b.first
            kA++
        } else if (x <= perfect1) {
            assignment = 1 or a.first or b.first
            break
        } else if (x < upper) {
            upper = x
            lower = sum - x
            assignment = 1 or a.first or b.first
            kB++
        } else {
            kB++
        }
    }

    return Pair(assignment, upper)
}

// TODO: since only n ≈ 20, a simple quadratic approach may be faster
// Karmarkar & Karp, 1982
private fun computeUpperbound(t: LongArray, sum: Long): Long {
    val q = PriorityQueue<Long>(t.size, reverseOrder())
    for (x in t) q.add(x)
    while (q.size > 1) q += q.poll() - q.poll()
    return (q.peek() + sum) / 2
}

private fun generateSubsets(t: LongArray, fromIndex: Int, toIndex: Int): MutableList<Schedule2> {
    val subsets = ArrayList<Schedule2>(1 shl toIndex - fromIndex)  // initial capacity: 2^jobs
    subsets += Pair(0, 0)
    for (i in fromIndex until toIndex) {
        for (sIdx in 0 until subsets.size) {  // manual iteration; otherwise ConcurrentModificationException is thrown
            val s = subsets[sIdx]
            subsets += Pair(1 shl i or s.first, t[i] + s.second)
        }
    }
    return subsets  // we do not remove subsets with equal sum since they are rare and it would be expensive
}
