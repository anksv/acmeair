/*******************************************************************************
* Copyright (c) 2013 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/
package com.acmeair.web;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import com.acmeair.entities.Customer;
import com.acmeair.entities.CustomerAddress;
import com.acmeair.service.*;
import com.acmeair.web.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Path("/api/customer")
public class CustomerREST {
	private static final Logger logger = LoggerFactory.getLogger(CustomerREST.class);
	static final String LOGIN_USER = "acmeair.login_user";

	@Autowired
	private CustomerService customerService;
	
	@Context 
	private HttpServletRequest request;


	private boolean validate(String customerid)	{
		String loginUser = request.getHeader(LOGIN_USER);
		return customerid.equals(loginUser);
	}
	@GET
	@Path("/byid/{custid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCustomer(@CookieParam("sessionid") String sessionid, @PathParam("custid") String customerid) {
		try {
			logger.info("Received request to get customer by id {} with session {}", customerid, sessionid);
			// make sure the user isn't trying to update a customer other than the one currently logged in
			if (!validate(customerid)) {
				logger.info("Customer id mismatched, requested = {}, logged = {}", customerid, request.getHeader("acmeair.login_user"));
				return Response.status(Response.Status.FORBIDDEN).build();

			}
			Customer customer = customerService.getCustomerByUsername(customerid);	
			CustomerInfo customerDTO = new CustomerInfo(customer);			
			return Response.ok(customerDTO).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@GET
	@Path("/{custid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCustomer(@PathParam("custid") String customerid) {
		try {
			Customer customer = customerService.getCustomerByUsername(customerid);
			CustomerInfo customerDTO = new CustomerInfo(customer);
			return Response.ok(customerDTO).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@POST
	@Path("/byid/{custid}")
	@Produces(MediaType.APPLICATION_JSON)
	public /* Customer */ Response putCustomer(@CookieParam("sessionid") String sessionid, CustomerInfo customer) {
		if (!validate(customer.getUsername())) {
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		
		Customer customerFromDB = customerService.getCustomerByUsernameAndPassword(customer.getUsername(), customer.getPassword());
		if (customerFromDB == null) {
			// either the customer doesn't exist or the password is wrong
			return Response.status(Response.Status.FORBIDDEN).build();
		}
		
		CustomerAddress addressFromDB = customerFromDB.getAddress();
		addressFromDB.setStreetAddress1(customer.getAddress().getStreetAddress1());
		if (customer.getAddress().getStreetAddress2() != null) {
			addressFromDB.setStreetAddress2(customer.getAddress().getStreetAddress2());
		}
		addressFromDB.setCity(customer.getAddress().getCity());
		addressFromDB.setStateProvince(customer.getAddress().getStateProvince());
		addressFromDB.setCountry(customer.getAddress().getCountry());
		addressFromDB.setPostalCode(customer.getAddress().getPostalCode());
		
		customerFromDB.setPhoneNumber(customer.getPhoneNumber());
		customerFromDB.setPhoneNumberType(Customer.PhoneType.valueOf(customer.getPhoneNumberType()));
		
		customerService.updateCustomer(customerFromDB);
		customerFromDB.setPassword(null);
		
		return Response.ok(customerFromDB).build();
	}
	

	
}
