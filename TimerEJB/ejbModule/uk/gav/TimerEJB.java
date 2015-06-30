package uk.gav;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

import org.codehaus.jackson.map.ObjectMapper;

import uk.gav.utilities.Environment;

@Stateless(name = "Timer")
public class TimerEJB implements TimerLocal {
	private final static String EVENT_DATASOURCE = Environment.class.getName() + ".DATA_SOURCE";

	@Resource
	private TimerService timerService;

	private Timer timer;

	private DataSource eventSource;

	private QueueConnectionFactory qconFactory;
	private Queue queue;
	private int maxEventBlock= JMSConstants.MAX_EMAIL_PER_CYCLE;

	private final static ObjectMapper mapper = new ObjectMapper();

	public TimerEJB() {
		try {
			Properties prop = Environment.getContextEnv();

			InitialContext context = new InitialContext(prop);

			// Grab the Datasource
			eventSource = (DataSource) context.lookup(prop.getProperty(EVENT_DATASOURCE));
			System.out.println("DATASOURCE located:: " + eventSource);

			qconFactory = (QueueConnectionFactory) context.lookup(JMSConstants.JMS_FACTORY);
			queue = (Queue) context.lookup(JMSConstants.JMS_EMAIL_Q);
		} catch (Exception e) {
			System.out.println("OH NO...data source cannot be found " + e);
		}

	}

	public void createTimer() {
		timer = timerService.createTimer(10000, 120000, null);
	}

	public void cancelTimer() {
		timer.cancel();
	};

	
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	private void stageEvents(List<EventEntity> eventList) throws Exception {

		QueueConnection qcon = qconFactory.createQueueConnection();
		QueueSession qsession = qcon.createQueueSession(false,
				Session.AUTO_ACKNOWLEDGE);
		QueueSender qsender = qsession.createSender(queue);
		TextMessage msg = qsession.createTextMessage();
		qcon.start();
		
		//Get ready to update status on initial table
		Connection c = eventSource.getConnection();
		PreparedStatement s = c
				.prepareStatement("UPDATE service_event_queue SET status = '1' WHERE id = ?");

		for (EventEntity ee:eventList) {
			String jsonContent = eventToJson(ee);
			msg.setText(jsonContent);
			qsender.send(msg);
			
			//Add the id to SQL batch
			s.setInt(1, ee.getId());
			s.addBatch();
		}
		
		int[] affectedRecords = s.executeBatch();
				
		qsender.close();
		qsession.close();
		qcon.close();
		c.close();
		
		System.out.println("Affected Records:::" + affectedRecords);
	}

	private static String eventToJson(EventEntity ee) throws Exception {
		return mapper.writeValueAsString(ee);
	}
	
	@Timeout
	public void timeout(Timer arg0) {
		System.out.println("recurring timer1 : " + new Date());

		try {
			Connection c = eventSource.getConnection();
			PreparedStatement s = c
					.prepareStatement("select event_id, event_type, event from service_event_queue where status IS NULL AND rownum <= ? FOR UPDATE");
			s.setInt(1, maxEventBlock);
			ResultSet events = s.executeQuery();

			List<EventEntity> eventList = new ArrayList<EventEntity>();
			while (events.next()) {
				EventEntity e = new EventEntity();
				e.setId(events.getInt(1));
				e.setType(events.getInt(2));
				e.setEventContent(events.getString(3));
				System.out.println("The event is:: " + e);
				eventList.add(e);
			}

			s.close();
			c.close();

 			stageEvents(eventList);

		} catch (Exception e) {
			System.out.println("Exception consuming current event batch:::" + e);
		}

	}
}
