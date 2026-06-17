/**
 * Focused REST inbound adapters grouped by documented HSMS contours. The
 * controllers keep HTTP concerns outside domain modules and call only public
 * module APIs or the gateway workflow service for cross-module use cases.
 */
@NamedInterface
package com.hsms.backend.api_gateway.controller;

import org.springframework.modulith.NamedInterface;