package johnshmo.woe.utils

class StateMachine<T> where T : Enum<T> {
    private var currentState: T? = null
    private val states: MutableMap<T, State<T>> = HashMap()

    var state: T?
        get() = currentState
        set(value) = changeState(value)

    operator fun get(state: T) = states[state]

    operator fun set(state: T, impl: State<T>?) {
        if (impl == null) {
            unregisterState(state)
            return
        }
        registerState(state, impl)
    }

    operator fun contains(state: T) = states.containsKey(state)

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

    fun registerState(state: T, impl: State<T>) {
        states[state] = impl
    }

    fun unregisterState(state: T) {
        states.remove(state)
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