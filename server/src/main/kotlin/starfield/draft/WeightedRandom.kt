package starfield.draft

class WeightedRandom<T>(choices: List<Pair<Int, T>>, private val withReplacement: Boolean = true) {
    private var totalWeight = choices.sumOf { it.first }
    private val choices = choices.toMutableList()

    fun pick(): T {
        if (withReplacement) {
            return getChoice()
        }
        val choice = getChoice()
        totalWeight -= choices.first { it.second == choice }.first
        choices.removeIf { it.second == choice }
        return choice
    }

    private fun getChoice(): T {
        val target = (0 until totalWeight).random()
        var sum = 0
        for ((weight, choice) in choices) {
            sum += weight
            if (sum > target) {
                return choice
            }
        }
        return choices.last().second
    }

}