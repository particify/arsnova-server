package net.particify.arsnova.authz.model

import java.util.Date
import java.util.UUID

data class LastAccess(val userId: UUID, val lastAccess: Date)
