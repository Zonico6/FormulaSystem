import java.lang.IllegalArgumentException

abstract class Formula<N, V>(val variables: Collection<N>) {
    fun canSolve(known: Map<N, V>): Boolean {
        val names = known.keys
        var overlap = 0
        for (e in variables) {
            if (names.contains(e)) {
                overlap += 1
            }
        }

        return overlap >= variables.size - 1
    }

    abstract fun solve(vars: Map<N, V>): Pair<N, V>?
}

class SimpleFormula<N, V>(variables: Collection<N>) : Formula<N, V>(variables) {

    private fun varIndexOf(name: N) = variables.indexOf(name)

    private val solves = Array<(known: Map<N, V>) -> V?>(variables.size) {{ _ -> null }}

    fun getSolves(): Map<N, (known: Map<N, V>) -> V?> {
        val pairs = variables.mapIndexed { i, e -> e to solves[i] }
        val ret = mutableMapOf<N, (known: Map<N, V>) -> V?>()
        pairs.forEach {
            ret[it.first] = it.second
        }
        return ret
    }

    fun addSolve(variable: N, func: (known: Map<N, V>) -> V?) {
        val index = varIndexOf(variable)
        if (index == -1)
            throw IllegalArgumentException("The variable that this solve was to be associated to is not registered within the formula.")
        solves[index] = func
    }
    fun removeSolve(variable: N) {
        solves[varIndexOf(variable)] = { null }
    }

    override fun solve(vars: Map<N, V>): Pair<N, V>? {
        if (!canSolve(vars))
            return null
        var unknown: N? = null
        for (e in variables) {
            if (!vars.containsKey(e)) {
                unknown = e
            }
        }
        if (unknown == null) return null
        val value = solves[varIndexOf(unknown)](vars)
        return if (value == null) null
        else unknown to value
    }
}

class FormulaSystem<N, V>(formulas: Iterable<Formula<N, V>>) {
    private val formulas = formulas.toMutableList()

    private val known: MutableMap<N, V> = mutableMapOf()
    val knownVariables get() = known as Map<N, V>

    /**
     * Adds the variables to the known variables and tries to calculate as many unknown as possible.
     *
     * @return True if at least one new variable was solved, i.e. the known have changed.
     */
    fun add(newVariables: Iterable<Pair<N, V>>): Boolean {
        known += newVariables

        var addedNewVar = true
        var foundNew = false
        while (addedNewVar) {
            addedNewVar = false

            val removeFormulas = mutableListOf<Formula<N, V>>()
            formulas.forEach {
                if (it.canSolve(known)) {
                    foundNew = true
                    val solved = it.solve(known)
                    if (solved != null) {
                        known += solved
                        addedNewVar = true
                    }
                    removeFormulas.add(it)
                }
            }
            formulas.removeAll(removeFormulas)
        }
        return foundNew
    }
    fun add(vararg newVariables: Pair<N, V>) = add(newVariables.toList())

    fun set(name: N, value: V) = add(name to value)
    fun set(variable: Pair<N, V>) = add(variable)
}