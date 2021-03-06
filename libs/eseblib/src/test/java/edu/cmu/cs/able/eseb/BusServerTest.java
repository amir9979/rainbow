package edu.cmu.cs.able.eseb;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.junit.Test;

import auxtestlib.AbstractControlledThread;
import auxtestlib.RandomGenerator;
import auxtestlib.TestPropertiesDefinition;
import edu.cmu.cs.able.eseb.bus.EventBus;
import edu.cmu.cs.able.typelib.prim.PrimitiveScope;

/**
 * Test class that makes basic checks on a bus server.
 */
public class BusServerTest extends EsebTestCase {
	@SuppressWarnings({"javadoc","unused"})
	@Test
	public void start_opens_ports_and_stop_closes() throws Exception {
		short p = (short) TestPropertiesDefinition.getInt(
				"free-port-zone-start");
		
		/*
		 * First try to connect to ensure the ports are closed.
		 */
		try {
			new Socket("localhost", p);
			fail();
		} catch (IOException e) {
			/*
			 * Expected.
			 */
		}
		
		/*
		 * Start the bus server and connect.
		 */
		try (EventBus bs = new EventBus(p, new PrimitiveScope());
				Socket s = new Socket("localhost", p)) {
			/*
			 * Wait a little bit.
			 */
			Thread.sleep(250);
			
			assertFalse(bs.closed());
			
			/*
			 * Shutting down the server should shut down the sockets.
			 */
			bs.close();
			assertTrue(bs.closed());
			
			/*
			 * Wait a little bit.
			 */
			Thread.sleep(250);
			
			try {
				s.getOutputStream().write(5);
				if (s.getInputStream().read() != -1) {
					fail();
				}
			} catch (IOException e) {
				/*
				 * Expected.
				 */
			}
		}
	}
	
	@SuppressWarnings({"javadoc","unused"})
	@Test
	public void can_start_multiple_servers_but_not_on_same_ports()
			throws Exception {
		short p1 = (short) TestPropertiesDefinition.getInt(
				"free-port-zone-start");
		short p2 = (short) (p1 + 1);
		
		try (EventBus b1 = new EventBus(p1, new PrimitiveScope())) {
			/*
			 * These should fail. 
			 */
			try {
				new EventBus(p1, new PrimitiveScope());
				fail();
			} catch (IOException e) {
				/*
				 * Expected.
				 */
			}
			
			/*
			 * This should succeed.
			 */
			try (EventBus b2 = new EventBus(p2, new PrimitiveScope())) {
				/*
				 * This one should fail now.
				 */
				try {
					new EventBus(p2, new PrimitiveScope());
					fail();
				} catch (IOException e) {
					/*
					 * Expected.
					 */
				}
				
			}
			/*
			 * But if we shutdown b2 we should be able to create it.
			 */
			try (EventBus b2 = new EventBus(p2, new PrimitiveScope())) {
				/*
				 * Nothing to do.
				 */
			}
		}
	}
	
	@SuppressWarnings("javadoc")
	@Test
	public void client_that_sends_garbage_is_disconnected() throws Exception {
		short p = (short) TestPropertiesDefinition.getInt(
				"free-port-zone-start");
		try (EventBus b = new EventBus(p, new PrimitiveScope())) {
			b.start();
			Thread.sleep(250);
			try (final Socket s = new Socket("localhost", p)) {
				s.setSoTimeout(25);
				
				AbstractControlledThread ct = new AbstractControlledThread() {
					@Override
					public Object myRun() throws Exception {
						byte[] rand = RandomGenerator.randBytes(1000);
						for (int i = 0; i < 1000; i++) {
							s.getOutputStream().write(rand);
							Thread.sleep(50);
						}
						
						return null;
					}
				};
				
				ct.start();
				
				/*
				 * We can read some stuff but the server will eventually close
				 * the connection (faster than the ping timeout).
				 */
				long now = System.currentTimeMillis();
				int r;
				do {
					r = 0;
					try {
						r = s.getInputStream().read();
					} catch (SocketTimeoutException e) {
						/*
						 * Expected.
						 */
					} catch (SocketException e) {
						/*
						 * This is another way of detecting an abrupt
						 * disconnect...
						 */
						r = -1;
					}
				} while (r != -1 && System.currentTimeMillis() - now < 5000);
				
				ct.waitForEnd();
				
				assertEquals(-1, r);
				assertTrue(ct.getDeathException() != null);
				s.close();
			}
		}
		
		Thread.sleep(250);
	}
}
