package exceptions

class UniqueNameNotFound: Exception {
    constructor() : super()
    constructor(message: String) : super(message)
}