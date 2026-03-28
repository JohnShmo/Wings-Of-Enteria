package johnshmo.woe.utils

interface StateInterface<T> where T : Enum<T> {
    fun enter()
    fun exit()
    fun advance(amount: Float): T?
}