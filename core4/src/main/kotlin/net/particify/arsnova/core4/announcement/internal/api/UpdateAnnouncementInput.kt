/* Copyright 2025 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.announcement.internal.api

import java.util.UUID

data class UpdateAnnouncementInput(val id: UUID, val title: String?, val body: String?)
