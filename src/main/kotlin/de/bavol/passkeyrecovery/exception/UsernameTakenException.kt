package de.bavol.passkeyrecovery.exception

class UsernameTakenException(username: String) :
    RuntimeException("Der Nutzername '$username' ist bereits vergeben.")