/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.playandgo.hsc.rest;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.smartcommunitylab.playandgo.hsc.error.DataException;
import it.smartcommunitylab.playandgo.hsc.error.HSCError;
import it.smartcommunitylab.playandgo.hsc.error.NotFoundException;
import it.smartcommunitylab.playandgo.hsc.error.OperationNotEnabledException;
import it.smartcommunitylab.playandgo.hsc.error.OperationNotPermittedException;

/**
 * @author raman
 *
 */
@ControllerAdvice
public class RestResponseEntityExceptionHandler 
  extends ResponseEntityExceptionHandler {


    @ExceptionHandler(NotFoundException.class)
    protected ResponseEntity<Object> handleInvalidPwd(
		NotFoundException ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorMsg(ex.getMessage(), ex.getDetails()), 
          new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(DataException.class)
    protected ResponseEntity<Object> handleLoginInUse(
    		DataException ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorMsg("Validation error", ex.getDetails()), 
          new HttpHeaders(), HttpStatus.CONFLICT, request);
    }
    @ExceptionHandler(OperationNotEnabledException.class)
    protected ResponseEntity<Object> handleLoginAnotherOrg(
      HSCError ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorMsg("Operation not enabled", ex.getDetails()), 
          new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
    @ExceptionHandler(OperationNotPermittedException.class)
    protected ResponseEntity<Object> handleAccountError(
      HSCError ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorMsg("Operation not permitted", ex.getDetails()), 
          new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }    

    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<Object> handleSecurity(
      Exception ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorMsg("Incorrect credentials", "BAD_CREDENTIALS"), 
          new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(SecurityException.class)
    protected ResponseEntity<Object> handleAccess(
      Exception ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorMsg("Insufficient rights", "INSUFFICIENT_RIGHTS"), 
          new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<Object> handleAccessDenied(
      Exception ex, WebRequest request) {
        return handleExceptionInternal(ex, new ErrorMsg("Insufficient rights", "INSUFFICIENT_RIGHTS"), 
          new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<Object> handleGeneric(
      Exception ex, WebRequest request) {
    	ex.printStackTrace();
        return handleExceptionInternal(ex, new ErrorMsg("Generic error", "GENERIC_ERROR"), 
          new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
    
    
    public static class ErrorMsg {
    	public String message;
    	public String type;
    	public Map<String, Object> errorData;

		public ErrorMsg(String message, String type) {
			super();
			this.message = message;
			this.type = type;
		}
		public ErrorMsg(String message, String type, Map<String, Object> errorData) {
			super();
			this.message = message;
			this.errorData = errorData;
		}

		public ErrorMsg() {
		}
    }
}