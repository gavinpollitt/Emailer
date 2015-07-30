package uk.gav.event.ejb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerService;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import uk.gav.event.EventEntity;
import uk.gav.event.constants.ContextConstants;
import uk.gav.event.constants.JMSConstants;
import uk.gav.event.utilities.Environment;
import uk.gav.event.utilities.MessageState;

/**
 * 
 * @author gavin
 *
 *         Implementation of a stateless session bean that requests the creation
 *         of a application server timer, which, a regular intervals will
 *         consume a batch of events and push onto the appropriate queues for
 *         the various event types. Trivial locking is put in place to only
 *         allow a single timer access to a set of events at one time.
 */
@SuppressWarnings("restriction")
@Stateless(name = "Timer")
public class TimerEJB implements TimerLocal {
	final static Logger log = Logger.getLogger(TimerEJB.class.getName());

	private final static String EVENT_DATASOURCE = Environment.class.getName()
			+ ".DATA_SOURCE";

	private final static String IDENTIFY_NEW_EVENTS = "SELECT event_id, event_type, event FROM service_event_queue WHERE status IS NULL AND rownum <= ? FOR UPDATE";

	private final static String CONFIRM_QUEUED_SQL = "UPDATE service_event_queue SET status = ? WHERE event_id = ?";

	// Injection of the timer service is provided by the container
	@Resource
	private TimerService timerService;

	private DataSource eventSource;

	private QueueConnectionFactory qconFactory;

	private Map<Integer, Queue> queues = new HashMap<Integer, Queue>();

	private int maxEventBlock = JMSConstants.MAX_EVENT_PER_CYCLE;

	private final static ObjectMapper mapper = new ObjectMapper();

	private InitialContext context;

	/**
	 * Identify the datasource and JMS connection factories utilising the system
	 * properties. In later Java/Weblogic versions, these can be injected.
	 */
	public TimerEJB() {
		try {
			Properties prop = Environment.getContextEnv();

			context = new InitialContext(prop);

			// Grab the Datasource
			eventSource = (DataSource) context.lookup(prop
					.getProperty(EVENT_DATASOURCE));
			log.debug("DATASOURCE located:: " + eventSource);

			qconFactory = (QueueConnectionFactory) context
					.lookup(JMSConstants.JMS_FACTORY_EVENT);
		} catch (Exception e) {
			log.error("Data source cannot be found " + e);
		}

	}

	/**
	 * 
	 * @param id
	 *            The id of the event type being processed
	 * @return The JMS queue responsible for this event type
	 * @throws Exception
	 *             If queue cannot be found If the queue has not already been
	 *             cached, identify it and cache for use now and in the future.
	 */
	private Queue identifyQueue(final Integer id) throws Exception {
		Queue q = queues.get(id);
		if (q == null) {
			q = (Queue) context.lookup("jms/"
					+ ContextConstants.EVENT_TYPES.get(id) + "Queue");
			queues.put(id, q);
		}

		return q;
	}

	/**
	 * Request from the container the creation of a timer with the name
	 * supplied.
	 */
	public void createTimer(final String id) {
		timerService.createTimer(ContextConstants.TIMER_TRIGGER_PERIOD,
				ContextConstants.TIMER_INTERVAL, id);
		log.info("Timer:" + id + " will be triggered after "
				+ (int) ContextConstants.TIMER_TRIGGER_PERIOD / 1000
				+ " seconds and " + (int) ContextConstants.TIMER_INTERVAL
				/ 1000 + " thereafter");
	}

	/**
	 * Cancel the time with the supplied name
	 */
	public void cancelTimer(final String id) {
		List<Timer> timers = getRunningTimers(new String[] { id });

		if (timers.size() > 0) {
			for (Timer t : timers) {
				t.cancel();
				log.info("Timer:" + id + " cancelled");
			}
		} else {
			throw new RuntimeException("Cannot locate timer:" + id
					+ " to cancel");
		}
	};

	/**
	 * Clear all active timers from the container
	 */
	public void cancelAllTimers() {
		List<Timer> timers = getRunningTimers();

		for (Timer t : timers) {
			log.info("Cancelled timer "
					+ (t.getInfo() != null ? t.getInfo() : t));
			t.cancel();
		}
	}

	/**
	 * 
	 * @param ids
	 *            A list of timer names to validate execution status
	 * @return The timers from the list that have been located.
	 */
	private List<Timer> getRunningTimers(final String[] ids) {
		Set<String> execs = new HashSet<String>(Arrays.asList(ids));
		List<Timer> foundTimers = new ArrayList<Timer>();

		Iterator<Timer> timers = getRunningTimers().iterator();
		while (timers.hasNext() && execs.size() > 0) {
			Timer t = timers.next();
			if (execs.contains(t.getInfo())) {
				foundTimers.add(t);
				execs.remove(t.getInfo());
			}
		}

		return foundTimers;
	}

	/**
	 * 
	 * @return All the timers currently running
	 */
	private List<Timer> getRunningTimers() {
		Iterator<?> timers = timerService.getTimers().iterator();

		List<Timer> outTimers = new ArrayList<Timer>();
		while (timers.hasNext()) {
			outTimers.add((Timer) timers.next());
		}

		return outTimers;
	}

	/**
	 * Returns all the timers currently executing
	 */
	public Set<String> getTimerIDs() {
		Set<String> ids = new HashSet<String>();
		for (Timer t : getRunningTimers()) {
			log.info("Located time with name::" + t.getInfo());

			if (t.getInfo() != null) {
				ids.add(t.getInfo().toString());
			} else {
				ids.add(t.toString());
			}
		}

		return ids;
	}

	/**
	 * For the list of events supplied, persist to the relevant JMS queue. This
	 * activity must be part of a transaction to ensure database and messaging
	 * maintains consistency.
	 * 
	 * @param eventList
	 *            The list of events to process
	 * @param timer
	 *            Used for logging purposes
	 * @throws Exception
	 *             If various JNDI targets cannot be located.
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	private void stageEvents(List<EventEntity> eventList, Timer timer)
			throws Exception {

		// Assume that we want this connection for the lifecycle of this set of
		// events
		// rather than having to worry about holding at class level and closing
		// at some point.
		QueueConnection qcon = qconFactory.createQueueConnection();
		QueueSession qsession = qcon.createQueueSession(false,
				Session.AUTO_ACKNOWLEDGE);

		// Get ready to update status on initial table
		Connection c = eventSource.getConnection();
		PreparedStatement s = c.prepareStatement(CONFIRM_QUEUED_SQL);

		s.setString(1, MessageState.NEW.progressState().getStateID().toString());

		for (EventEntity ee : eventList) {
			// Determine destination queue based on the event type
			QueueSender qsender = qsession.createSender(identifyQueue(ee
					.getType()));
			TextMessage msg = qsession.createTextMessage();
			qcon.start();

			// Convert the whole event to a JSON string for storage
			String jsonContent = eventToJson(ee);
			msg.setText(jsonContent);
			qsender.send(msg);
			qsender.close();

			// Add the id to SQL batch
			s.setInt(2, ee.getId());
			s.addBatch();
		}

		int[] affectedRecords = s.executeBatch();

		qsession.close();
		qcon.close();
		c.close();

		log.info("Timer: " + timer.getInfo() + ", Staged Records:::"
				+ affectedRecords.length);
	}

	private static String eventToJson(EventEntity ee) throws Exception {
		return mapper.writeValueAsString(ee);
	}

	/**
	 * This is the method that the container will call when a timer event is
	 * fired The queue table will be read (for the required maximum records)
	 * into memory in readiness for staging on the queues.
	 * 
	 * @param timer
	 *            The timer object that triggered this event.
	 */
	@Timeout
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void timeout(Timer timer) {
		log.info("Timer: " + timer.getInfo() + " woken up");

		try {
			Connection c = eventSource.getConnection();
			// Ensure no contesting over the data, by only allowing one thread
			// to read at a time.
			c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			PreparedStatement s = c.prepareStatement(IDENTIFY_NEW_EVENTS);
			s.setInt(1, maxEventBlock);
			ResultSet events = s.executeQuery();

			long rows = 0;
			int start = -1, end = 0;
			List<EventEntity> eventList = new ArrayList<EventEntity>();
			while (events.next()) {
				EventEntity e = new EventEntity();
				e.setId(events.getInt(1));
				e.setType(events.getInt(2));
				e.setEventContent(events.getString(3));
				eventList.add(e);

				start = start < 0 ? events.getInt(1) : start;
				end = events.getInt(1);
				rows++;
			}

			String inf = "Timer: " + timer.getInfo() + " read: " + rows;
			if (rows > 0) {
				inf += ", start id: " + start + ", end id: " + end;
			}
			log.info(inf);

			s.close();
			c.close();

			if (rows > 0) {
				stageEvents(eventList, timer);
			}
		} catch (Exception e) {
			log.error(timer.getInfo() + ":Exception consuming current event batch:::" + e);
		}

		log.info("Timer: " + timer.getInfo() + " going back to sleep");

	}
}
