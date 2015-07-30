package uk.gav.event.web;

import java.util.Properties;
import java.util.Set;

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

import org.apache.log4j.Logger;

import uk.gav.event.ejb.TimerLocal;
import uk.gav.event.utilities.Environment;

/**
 * RESTful Web Service definition to allow external creation, validation and
 * deletion of event timers. At present, EJB injection is restricted by version
 * of Weblogic. Injection properties have been retained, but are impotent at the
 * moment. EJB has to be injected programmatically.
 * 
 * @author gavin
 *
 */
@Path("/execution")
public class EventController {
	final static Logger log = Logger.getLogger(EventController.class.getName());

	@EJB(mappedName = "java:com/env/ejb/TimerLocal")
	TimerLocal timer;

	/**
	 * Performs EJB injection directly due to Weblogic version struggling with it.
	 */
	private void injectTimerManuallyUntilAutoWorks() {
		log.debug("Timer bean injected is::" + timer);

		Properties env = Environment.getContextEnv();

		try {
			InitialContext context = new InitialContext(env);
			// printJNDITree(context, 0, "java:comp/env");
			timer = (TimerLocal) context.lookup("java:comp/env/ejb/TimerLocal");
		} catch (Exception e) {
			log.error("Exception getting TimerLocal EJB:::" + e);
		}
	}

	/**
	 * RESTful post to create an execution of a timer as defined in the provided JSON.
	 * The environment data is also refreshed on creation of a timer.
	 * http://<host>:<port>/<EAR context>/execution
	 * JSON: {"id","<timer name>"}
	 * @param ee
	 */
	@POST
	public void addExecution(EventExecution ee) {
		injectTimerManuallyUntilAutoWorks();
		timer.createTimer(ee.getId());
		Environment.reload();
		log.debug("RESTful POST completed to create execution timer "
				+ ee.getId());
	}

	/**
	 * RESTful GET to acquire the execution status of the provided timer id
	 * http://<host>:<port>/<EAR context>/execution/<timer id>
	 * 
	 * @param msg The id of the timer to examine
	 * @return Response to user indicating the status of the timer
	 */
	@GET
	@Path("/{timer}")
	public Response printStatus(@PathParam("timer") String msg) {

		injectTimerManuallyUntilAutoWorks();
		Set<String> ids = timer.getTimerIDs();

		String output = "";
		if (ids.contains(msg)) {
			output = "Execution:" + msg + " is currently running\n";
		} else {
			output = "Execution:" + msg + " is not running\n";
		}
		log.debug("RESTful GET completed to give timer status for " + msg);
		return Response.status(200).entity(output).build();

	}

	/**
	 * RESTful delete to remove the provided timer id
	 * http://<host>:<port>/<EAR context>/execution/<timer id>
	 * 
	 * @param id The id of the timer to examine
	 */
	@DELETE
	@Path("/{id}")
	public void removeExecution(@PathParam("id") String id) {
		injectTimerManuallyUntilAutoWorks();

		if (id.equals("ALL_TIMERS")) {
			timer.cancelAllTimers();
		} else {
			timer.cancelTimer(id);
		}
		log.debug("RESTful DELETE completed to remove execution timer " + id);
	}

	
	public void printJNDITree(InitialContext context, int pad, String ct) {
		try {
			printNE(context, pad, context.list(ct), ct);
		} catch (NamingException e) {
			// ignore leaf node exception
		}
	}

	private void printNE(InitialContext context, int pad, NamingEnumeration<NameClassPair> ne,
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
