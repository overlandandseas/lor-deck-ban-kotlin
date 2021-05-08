class RoomAlreadyExistsException(message: String) : Exception(message)

class RoomIsFullException(message: String) : Exception(message)

class RoomNotFoundException(message: String) : Exception(message)

class PlayerNotFoundException(message: String) : Exception(message)

class PlayerAlreadySubmittedDecksException(message: String) : Exception(message)

class PlayerAlreadySubmittedBansException(message: String) : Exception(message)

class UserHeaderNotFoundException(message: String) : Exception(message)