package johnshmo.woe.utils

class StateMachine<T> where T : Enum<T> {
    private var currentState: T? = null
    private val states: MutableMap<T, StateInterface<T>> = HashMap()

    var state: T?
        get() = currentState
        set(value) = changeState(value)

    fun changeState(newState: T?) {
        if (currentState != null) {
            states[currentState]?.exit()
        }
        if (newState == null) {
            currentState = null
            return
        }
        currentState = newState
        states[newState]?.enter()
    }

    fun advance(amount: Float) {
        if (currentState == null) return
        val nextState = states[currentState]?.advance(amount)
        if (nextState != null && nextState != currentState) {
            changeState(nextState)
        }
    }

    fun registerState(state: T, stateInterface: StateInterface<T>) {
        states[state] = stateInterface
    }

    fun unregisterState(state: T) {
        states.remove(state)
    }
}