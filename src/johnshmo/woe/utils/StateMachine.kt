package johnshmo.woe.utils

class StateMachine<T> where T : Enum<T> {
    private var currentState: T? = null
    private val states: MutableMap<T, State<T>> = HashMap()

    var stateId: T?
        get() = currentState
        set(value) = changeState(value)

    operator fun get(stateId: T) = states[stateId]

    operator fun set(stateId: T, impl: State<T>?) {
        if (impl == null) {
            unregisterState(stateId)
            return
        }
        registerState(stateId, impl)
    }

    operator fun contains(stateId: T) = states.containsKey(stateId)

    fun changeState(newState: T?) {
        if (currentState == newState) return
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
        if (nextState != null) {
            changeState(nextState)
        }
    }

    fun registerState(stateId: T, state: State<T>) {
        states[stateId] = state
    }

    fun unregisterState(stateId: T) {
        states.remove(stateId)
    }

    fun getCurrent(): State<T>? {
        if (currentState == null) return null
        return states[currentState]
    }

    interface State<T> where T : Enum<T> {
        fun enter()
        fun exit()
        fun advance(amount: Float): T?
    }
}