package de.bavol.passkeyrecovery.exception

class CredentialNotFoundException() :
    RuntimeException("Kein Credential mit dieser ID gefunden.")