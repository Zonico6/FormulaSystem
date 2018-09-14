import kotlin.math.*

fun parseGMArg(arg: String): Double =
        when (arg) {
            "GM-kerbin" -> GMKerbin
            "GM-earth" -> GMEarth
            "GM-Mun" -> GMMun
            else -> arg.toDouble()
        }
fun parseHeightArg(arg: String): Double =
        when (arg) {
            "geosync-kerbin" -> geosyncKerbin.toDouble()
            "geostationary" -> geostationary.toDouble()
            else -> arg.toDouble()
        }

fun parseArg(arg: String): Double =
        when (arg) {
            "GM-kerbin" -> GMKerbin
            "GM-earth" -> GMEarth
            "GM-Mun" -> GMMun
            "geosync-kerbin" -> geosyncKerbin.toDouble()
            "geostationary" -> geostationary.toDouble()
            else -> arg.toDouble()
        }
fun parseInsertionArgs(args: List<String>): List<Pair<String, Double>> {
    val startRadius = parseHeightArg(args[0])
    val GMVal = parseGMArg(if (args.size > 2) args[2] else "GM-kerbin")
    val basePeriod = (4 * PI * PI * startRadius.toFloat().pow(3) / GMVal).pow(1 / 2.toDouble())

    val ret = mutableListOf<Pair<String, Double>>()
    ret.add(periRad to startRadius)
    ret.add(GM to GMVal)
    ret.add(period to basePeriod * args[1].eval())

    return ret
}

fun String.eval(): Double {
    fun calculation(operator: String, calc: (a: Double, b: Double) -> Double): Double {
        val parts = split(operator)
        return parts.asSequence().map { it.eval() }.reduce { acc, e ->
            calc(acc, e)
        }
    }
    return when {
        contains('+') -> calculation("+") { a, b -> a + b}
        contains('-') -> calculation("-") { a, b -> a - b}
        contains('/') -> calculation("/") { a, b -> a / b}
        contains('*') -> calculation("*") { a, b -> a * b}
        else -> toDouble()
    }
}

fun main(args: Array<String>) {
    val system = constructOrbitSystem()

    if (args.isNotEmpty()) {

        when (args[1]) {
            "--deploy-orbit", "-do" -> {
                system.add(parseInsertionArgs(args.slice(2..args.size)))
            }
            /*"--direct", "-d",*/
            else -> {
                for (arg in args.slice(0..args.size)) {
                    if (arg[0] == '-')
                        continue
                    else {
                        val (name, item) = arg.split('=', ':')
                        val value = parseArg(item)
                        system.set(name to value)
                    }
                }
            }
        }
    } else {
        println("Do you want to calculate a insertion orbit of another orbit? " +
                "(Yes/No), No will let you specify all parameters by yourself:")
        if (readLine()!!.toLowerCase() == "yes") {
            println("Nice, please specify your parameters in the format: \n" +
                    "\"height period_portion GM\". \nIf you don't provide a GM, kerbin's is used:")
            system.add(parseInsertionArgs(readLine()!!.split(' ')))
        } else {
            println("Okay, now you can go ahead and just type all the variables with their respective values in the format: \n" +
                    "name:value or name=value\n" +
                    "If you want to leave at any point, just type in \"exit\".")
            while (true) {
                val input = readLine()!!
                if (input.toLowerCase() == "exit") {
                    break
                } else {
                    val (name, item) = input.split('=', ':')
                    val value = parseArg(item)
                    system.set(name to value)
                }
            }
        }
    }

    if (!system.knownVariables.containsKey(GM)) {
        system.set(GM to GMKerbin)
    }

    println(system.knownVariables.toString())
}

const val GMEarth = 3.986004418 * 100000000000000
const val GMKerbin = 3.5316 * 1000000000000
const val GMMun = 6.5138398 * 10000000000

const val geosyncKerbin = 3463330
const val geostationary = 35786000

const val period = "period"
const val periRad = "Rp"
const val periVel = "Vp"
const val apoRad = "Ra"
const val apoVel = "Va"
const val eccentricity = "e"
const val semiMajor = "a"
const val GM = "GM"

typealias OrbitFormula = SimpleFormula<String, Double>

fun constructOrbitSystem(): FormulaSystem<String, Double> {
    val formulas: MutableList<Formula<String, Double>> = mutableListOf()

    var formula = OrbitFormula(listOf(GM, periVel, periRad, apoRad))
    formula.addSolve(periVel) {
        val GM = it.getValue(GM)
        val Rp = it.getValue(periRad)
        val Ra = it.getValue(apoRad)
        sqrt(2 * GM * Ra / (Rp * (Rp + Ra)))
    }
    formulas.add(formula)

    formula = SimpleFormula(listOf(GM, periRad, apoRad, apoVel))
    formula.addSolve(apoVel) {
        val periRad = it.getValue(periRad)
        val apoRad = it.getValue(apoRad)
        sqrt(2 * it.getValue(GM) * periRad / (apoRad * (apoRad + periRad)))
    }
    formulas.add(formula)

    formula = SimpleFormula(listOf(GM, period, semiMajor))
    formula.addSolve(semiMajor) {
        val GM = it.getValue(GM)
        val period = it.getValue(period)

        (GM * period * period / (4 * PI * PI)).pow(1/3.toDouble())
    }
    formulas.add(formula)

    formulas.add(OrbitFormula(listOf(semiMajor, apoRad, periRad)).apply {
        addSolve(periRad) {
            it.getValue(semiMajor) * 2 - it.getValue(apoRad)
        }
        addSolve(apoRad) {
            it.getValue(semiMajor) * 2 - it.getValue(periRad)
        }
    })

    formula = SimpleFormula(listOf(periRad, semiMajor, eccentricity))
    formula.addSolve(periRad) {
        it.getValue(semiMajor) * (1 - it.getValue(eccentricity))
    }
    formula.addSolve(semiMajor) {
        it.getValue(periRad) / (1 - it.getValue(eccentricity))
    }
    formulas.add(formula)

    formula = SimpleFormula(listOf(eccentricity, apoRad, periRad))
    formula.addSolve(eccentricity) {
        1 - (2 / (it.getValue(apoRad) / it.getValue(periRad) + 1))
    }
    formulas.add(formula)

    return FormulaSystem(formulas)
}