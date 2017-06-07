package de.htwg_konstanz.ebus.wholesaler.rs.demo;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
 
@Path("/hello")
public class WholesalerRestDemo {
 
	@GET
	@Path("/{param}")
	public Response getMsg(@PathParam("param") String msg) {
 
		String output = "Jersey says: " + msg;
 
		return Response.status(200).entity(output).build(); 
	}
	
	@GET
	@Path("/square/{param}")
	public Response getSquare(@PathParam("param") int num) {
 
		String output = "Square of " + num + " is " + (num*num);
 
		return Response.status(200).entity(output).build(); 
	}
	
	@POST
	@Consumes("text/plain")
	@Path("/squareroot")
	public Response getSquareRoot(final String num) {
 
		Integer x = Integer.valueOf(num);

		String output = "Squareroot of " + x + " is " + Math.sqrt(x);
 
		return Response.status(200).entity(output).build(); 
	}
 
}
