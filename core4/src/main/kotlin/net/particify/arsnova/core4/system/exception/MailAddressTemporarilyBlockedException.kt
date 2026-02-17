/* Copyright 2026 Particify GmbH
 * SPDX-License-Identifier: MIT
 */
package net.particify.arsnova.core4.system.exception

class MailAddressTemporarilyBlockedException :
    RuntimeException("Please wait before sending another mail.")
