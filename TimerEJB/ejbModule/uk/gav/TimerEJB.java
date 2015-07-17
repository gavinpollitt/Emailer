package uk.gav;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

import uk.gav.utilities.Environment;

@Stateless(name = "Timer")
public class TimerEJB implements TimerLocal {
	final static Logger log = Logger.getLogger(TimerEJB.class.getName());

	private final static String EVENT_DATASOURCE = Environment.class.getName()
			+ ".DATA_SOURCE";

	@Resource
	private TimerService timerService;

	private DataSource eventSource;

	private QueueConnectionFactory qconFactory;
	private Queue queue;
	private int maxEventBlock = JMSConstants.MAX_EMAIL_PER_CYCLE;

	private final static ObjectMapper mapper = new ObjectMapper();

	public TimerEJB() {
		try {
			Properties prop = Environment.getContextEnv();

			InitialContext context = new InitialContext(prop);

			// Grab the Datasource
			eventSource = (DataSource) context.lookup(prop
					.getProperty(EVENT_DATASOURCE));
			log.debug("DATASOURCE located:: " + eventSource);

			qconFactory = (QueueConnectionFactory) context
					.lookup(JMSConstants.JMS_FACTORY);
			queue = (Queue) context.lookup(JMSConstants.JMS_EMAIL_Q);
		} catch (Exception e) {
			log.error("Data source cannot be found " + e);
		}

	}

	public void createTimer(String id) {
		timerService.createTimer(10000, 120000, id);
	}

	public void cancelTimer(String id) {
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

	public void cancelAllTimers() {
		List<Timer> timers = getRunningTimers();

		for (Timer t : timers) {
			log.info("Cancelled timer "
					+ (t.getInfo() != null ? t.getInfo() : t));
			t.cancel();
		}
	}

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

	private List<Timer> getRunningTimers() {
		Iterator<?> timers = timerService.getTimers().iterator();

		List<Timer> outTimers = new ArrayList<Timer>();
		while (timers.hasNext()) {
			outTimers.add((Timer) timers.next());
		}

		return outTimers;
	}

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

	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	private void stageEvents(List<EventEntity> eventList, Timer timer)
			throws Exception {

		QueueConnection qcon = qconFactory.createQueueConnection();
		QueueSession qsession = qcon.createQueueSession(false,
				Session.AUTO_ACKNOWLEDGE);
		QueueSender qsender = qsession.createSender(queue);
		TextMessage msg = qsession.createTextMessage();
		qcon.start();

		// Get ready to update status on initial table
		Connection c = eventSource.getConnection();
		PreparedStatement s = c
				.prepareStatement("UPDATE service_event_queue SET status = '1' WHERE event_id = ?");

		for (EventEntity ee : eventList) {
			String jsonContent = eventToJson(ee);
			msg.setText(jsonContent);
			qsender.send(msg);

			// Add the id to SQL batch
			s.setInt(1, ee.getId());
			s.addBatch();
		}

		int[] affectedRecords = s.executeBatch();

		qsender.close();
		qsession.close();
		qcon.close();
		c.close();

		log.info("Timer: " + timer.getInfo() + ", Staged Records:::"
				+ affectedRecords.length);
	}

	private static String eventToJson(EventEntity ee) throws Exception {
		return mapper.writeValueAsString(ee);
	}

	@Timeout
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void timeout(Timer timer) {
		log.info("Timer: " + timer.getInfo() + " woken up");

		try {
			Connection c = eventSource.getConnection();
			// Ensure no contesting over the data, by only allowing one thread
			// to read at a time.
			c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			PreparedStatement s = c
					.prepareStatement("select event_id, event_type, event from service_event_queue where status IS NULL AND rownum <= ? FOR UPDATE");
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
				System.out.println("The event is:: " + e);
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
			log.error("Exception consuming current event batch:::" + e);
		}

		log.info("Timer: " + timer.getInfo() + " going back to sleep");

	}
}
