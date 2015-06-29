package uk.gav;

import java.util.GregorianCalendar;
import java.util.Properties;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import uk.gav.utilities.Environment;

@Path("/execution")
public class EventController /* extends Application */{
	@EJB(mappedName = "java:com/env/ejb/TimerLocal")
	TimerLocal timer;

	private void injectTimerManuallyUntilAutoWorks() {
		System.out.println("Timer injected is::" + timer);

		Properties env = Environment.getContextEnv();

		try {
			InitialContext context = new InitialContext(env);
			printJNDITree(context, 0, "java:comp/env");
			timer = (TimerLocal) context.lookup("java:comp/env/ejb/TimerLocal");
		} catch (Exception e) {
			System.out.println("Exception getting TimerLocal EJB:::" + e);
		}
		System.out.println("Timer grabbed is::" + timer);

	}

	@POST
	public void addExecution(EventExecution ee) {
		ee.setTimestamp(GregorianCalendar.getInstance().getTime());
		System.out.println("The value is::" + ee.getTimestamp());
		injectTimerManuallyUntilAutoWorks();
		timer.createTimer();
	}

	@GET
	@Path("/{param}")
	public Response printMessage(@PathParam("param") String msg) {

		String result = "Restful example : " + msg;

		return Response.status(200).entity(result).build();

	}

	@DELETE
	@Path("/{id}")
	public void removeExecution(@PathParam("id") String id) {
		injectTimerManuallyUntilAutoWorks();
		timer.cancelTimer();
	}

	public void printJNDITree(InitialContext context, int pad, String ct) {
		try {
			printNE(context, pad, context.list(ct), ct);
		} catch (NamingException e) {
			// ignore leaf node exception
		}
	}

	private void printNE(InitialContext context, int pad, NamingEnumeration ne,
			String parentctx) throws NamingException {
		while (ne.hasMoreElements()) {
			NameClassPair next = (NameClassPair) ne.nextElement();
			printEntry(pad, next);
			printJNDITree(context, pad + 2,
					(parentctx.length() == 0) ? next.getName() : parentctx
							+ "/" + next.getName());
		}
	}

	private void printEntry(int pad, NameClassPair next) {
		String padder = "---------------------------------------------"
				.substring(0, pad);
		System.out.println(padder + "-->" + next);
	}

}
