/**
 * This package represents API Gateway module. It must be used for all
 * HTTP/HTTPS REST requests from all users. Controllers use special API interfaces
 * of other modules to call their services and methods.
 * One of the important keys is that JPA entities are private for their modules:
 * API Gateway and other modules must use data access objects of classes in modules
 * public apis.
 */

@ApplicationModule
package com.hsms.backend.api_gateway;

import org.springframework.modulith.ApplicationModule;