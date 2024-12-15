package starfield.model

import starfield.plugins.Location

abstract class ActiveUserCollection<out T : ActiveCollectionState> : UserCollection<T>() {
    abstract fun end(user: User): Boolean
    abstract fun location(): Location
}