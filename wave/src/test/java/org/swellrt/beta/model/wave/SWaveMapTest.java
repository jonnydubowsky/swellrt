package org.swellrt.beta.model.wave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SMutationHandler;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNodeAccessControl;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.local.SMapLocal;
import org.swellrt.beta.model.wave.mutable.SWaveMap;
import org.waveprotocol.wave.client.common.util.CountdownLatch;
import org.waveprotocol.wave.model.util.CollectionUtils;

import com.google.gwt.user.client.Command;


/**
 * Test SMapRemote
 */
public class SWaveMapTest extends SWaveNodeAbstractTest {


  protected void populatePrimitiveValues(SMap map) throws SException {

    map.put("k0", new SPrimitive("A value for k0", new SNodeAccessControl()));
    map.put("k1", "A value for k1");

  }

  protected void assertPrimitiveValues(SMap map) throws SException {

    assertEquals("A value for k0", SPrimitive.asString(map.pick("k0")));
    assertEquals("A value for k1", SPrimitive.asString(map.pick("k1")));

  }

  /**
   * Put only primitive values in the root map.
   *
   * Get primitive values with or without map cache
   * to force deserialization from Wave documents (XML)
   * @throws IllegalCastException
   *
   */
  public void testMapWithPrimitiveValues() throws SException {


    populatePrimitiveValues(object);
    assertPrimitiveValues(object);

    object.clearCache();

    populatePrimitiveValues(object);
    assertPrimitiveValues(object);

  }

  /**
   * Put a nested map in root map.
   * Put primitive values in inner map.
   *
   * Get primitive values with or without map cache to
   * force deserialization from Wave documents (XML)
   * @throws IllegalCastException
   *
   */
  public void testMapWithNestedMap() throws SException {


    SMap submap = new SMapLocal();

    populatePrimitiveValues(submap);

    object.put("submap", submap);

    assertPrimitiveValues((SMap) object.pick("submap"));


  }

  /**
   * Create two nested local maps<br>
   * Add them to a live object (so create remote maps)<br>
   * Check primitive values are retrieved from remote maps<br>
   * Make changes directly in remote maps<br>
   *
   * @throws SException
   */
  public void testMapWithNestedLocalMap() throws SException {

    // Create maps
    SMap mapA = new SMapLocal();
    populatePrimitiveValues(mapA);

    SMap mapB = new SMapLocal();
    populatePrimitiveValues(mapB);

    mapA.put("mapB", mapB);
    object.put("mapA", mapA);


    // Clear cache and check retrieving values
    object.clearCache();

    SMap remoteMapA = (SMap) object.pick("mapA");
    assertPrimitiveValues(remoteMapA);
    SMap remoteMapB = (SMap) remoteMapA.pick("mapB");
    assertPrimitiveValues(remoteMapB);

    // Make changes, no exceptions must be thrown
    remoteMapA.put("kA1", "Some value kA1");
    remoteMapB.put("kB1", "Some value kB1");

    object.clearCache();

    assertEquals("Some value kA1", SPrimitive.asString(remoteMapA.pick("kA1")));
    assertEquals("Some value kB1", SPrimitive.asString(remoteMapB.pick("kB1")));

  }

  /**
   * Test map events generated by local changes.
   *
   * @throws SException
   * @throws InterruptedException
   */
  public void testMapEvents() throws SException, InterruptedException {


    List<SEvent> recvEvents = new ArrayList<SEvent>();

    SMutationHandler eventHandler = new SMutationHandler() {

      @Override
      public boolean exec(SEvent e) {
        recvEvents.add(e);
        synchronized (this) {
          if (recvEvents.size() == 3)
            notifyAll();
        }

        return false;
      }

    };

    SMap map = new SMapLocal();
    populatePrimitiveValues(map);
    object.put("map", map);
    SWaveMap remoteMap = (SWaveMap) object.pick("map");
    remoteMap.addListener(eventHandler, null);

    remoteMap.remove("k1");
    remoteMap.put("k2" , "This is new value");
    remoteMap.put("k0" , "This is updated value");

    synchronized (eventHandler) {
      eventHandler.wait(1000);
    }

    // Check whether mutations were properly done
    assertEquals(2, remoteMap.size());
    assertEquals(null, remoteMap.pick("k1"));
    assertEquals("This is new value", SPrimitive.asString(remoteMap.pick("k2")));
    assertEquals("This is updated value", SPrimitive.asString(remoteMap.pick("k0")));

    // Check events
    assertEquals(3, recvEvents.size());

    assertEquals(SEvent.REMOVED_VALUE, recvEvents.get(0).getType());
    assertEquals("k1", recvEvents.get(0).getKey());
    assertEquals("A value for k1", (String) recvEvents.get(0).getValue());

    assertEquals(SEvent.ADDED_VALUE, recvEvents.get(1).getType());
    assertEquals("k2", recvEvents.get(1).getKey());
    assertEquals("This is new value", (String) recvEvents.get(1).getValue());

    assertEquals(SEvent.UPDATED_VALUE, recvEvents.get(2).getType());
    assertEquals("k0", recvEvents.get(2).getKey());
    assertEquals("This is updated value", (String) recvEvents.get(2).getValue());
  }


  /**
   * Test whether events generated by map changes are properly
   * propagated upwards within nested maps.
   * @throws SException
   * @throws InterruptedException
   */
  public void testMapEventsPropagation() throws SException, InterruptedException {

    List<SEvent> capturedEventsRoot = new ArrayList<SEvent>();
    List<SEvent> capturedEventsMapA = new ArrayList<SEvent>();
    List<SEvent> capturedEventsMapB = new ArrayList<SEvent>();

    CountdownLatch cd = CountdownLatch.create(5, new  Command() {

      @Override
      public void execute() {

        // Case 1) Generate event in C
        // captured by handlerB but not in handlerA and handlerRoot
        SEvent e = capturedEventsMapB.get(0);
        assertNotNull(e);
        assertEquals(SEvent.ADDED_VALUE, e.getType());
        assertEquals("valueForC", (String) e.getValue());

        // Case 2) Generate event in B
        // captured by handlerB but not in handlerA and handlerRoot
        e = capturedEventsMapB.get(1);
        assertNotNull(e);
        assertEquals(SEvent.ADDED_VALUE, e.getType());
        assertEquals("valueForB", (String) e.getValue());

        // Case 3) Generate event in A
        // captured by handlerA and rootHandler
        e = capturedEventsMapA.get(0);
        assertNotNull(e);
        assertEquals(SEvent.ADDED_VALUE, e.getType());
        assertEquals("valueForA", (String) e.getValue());
        assertEquals(1, capturedEventsMapA.size()); // Assert case 1 and case 2, "but" parts

        e = capturedEventsRoot.get(0);
        assertNotNull(e);
        assertEquals(SEvent.ADDED_VALUE, e.getType());
        assertEquals("valueForA", (String) e.getValue());
        assertEquals(1, capturedEventsRoot.size()); // Assert case 1 and case 2, "but" parts

      }
    });



    SMutationHandler handlerRoot = new SMutationHandler() {

      @Override
      public boolean exec(SEvent e) {
        capturedEventsRoot.add(e);
        // System.out.println("handlerRoot: "+e.toString());
        cd.tick();
        // for root handler this has not actual effect
        return true;
      }

    };

    SMutationHandler handlerMapA = new SMutationHandler() {

      @Override
      public boolean exec(SEvent e) {
        capturedEventsMapA.add(e);
        // System.out.println("handlerMapA: "+e.toString());
        cd.tick();
        // this handler will let
        // events to flow upwards
        return true;
      }

    };

    SMutationHandler handlerMapB = new SMutationHandler() {

      @Override
      public boolean exec(SEvent e) {
        capturedEventsMapB.add(e);
        // System.out.println("handlerMapB: "+e.toString());
        cd.tick();
        // this handler won't let
        // events to flow upwards
        return false;
      }

    };

    // Create test data and bind event listeners

    // SMap map = new SMapLocal();
    //populatePrimitiveValues(map);

    //
    // root map <- hanlderRoot
    //   |
    //   -- mapA  <- handlerA (allow propagation)
    //       |
    //       -- mapB <- handlerB (stop propagation)
    //           |
    //           -- mapC
    //

    SWaveMap remoteMapA = (SWaveMap) object.put("mapA", new SMapLocal()).pick("mapA");
    SWaveMap remoteMapB = (SWaveMap) remoteMapA.put("mapB", new SMapLocal()).pick("mapB");
    SWaveMap remoteMapC = (SWaveMap) remoteMapB.put("mapC", new SMapLocal()).pick("mapC");

    // Set handlers here to ignore events for initialization fields
    remoteMapA.addListener(handlerMapA, null);
    remoteMapB.addListener(handlerMapB, null);
    object.addListener(handlerRoot, null);

    // Case 1) Generate event in C
    // captured by handlerB but not in handlerA and handlerRoot
    remoteMapC.put("keyForC", "valueForC");

    // Case 2) Generate event in B
    // captured by handlerB but not in handlerA and handlerRoot
    remoteMapB.put("keyForB", "valueForB");

    // Case 3) Generate event in A
    // captured by handlerA and rootHandler
    remoteMapA.put("keyForA", "valueForA");

    cd.tick();

  }



  public void testAccessControl() throws SException {

    // field without access control

    SNodeAccessControl nac = new SNodeAccessControl();
    SPrimitive p = new SPrimitive("total access", nac);
    object.put("prop1", p);

    try {
      object.pick("prop1");
    } catch (Exception e) {
      assertTrue("SException not expected", false);
    }

    try {
      object.put("prop1", p);
    } catch (Exception e) {
      assertTrue("SException not expected", false);
    }

    try {
      object.remove("prop1");
    } catch (Exception e) {
      assertTrue("SException not expected", false);
    }

    // field read only, any user

    nac = new SNodeAccessControl(true);
    p = new SPrimitive("read only access", nac);
    object.put("prop2", p);

    try {
      object.pick("prop2");
    } catch (Exception e) {
      assertTrue("SException not expected", false);
    }

    try {
      object.put("prop2", p);
    } catch (SException e) {
      assertTrue("SException expected, can't write", true);
    }

    try {
      object.remove("prop2");
    } catch (SException e) {
      assertTrue("SException expected, can't write", true);
    }


    // field read only, restricted user list (current user in list)

    nac = new SNodeAccessControl(CollectionUtils.immutableSet("tom@acme.com","ann@acme.com") ,Collections.<String>emptySet(), true);
    p = new SPrimitive("read only with access list", nac);
    object.put("prop3", p);

    try {
      object.pick("prop3");
    } catch (Exception e) {
      assertTrue("SException not expected", false);
    }

    try {
      object.put("prop3", p);
    } catch (SException e) {
      assertTrue("SException expected, can't write", true);
    }

    try {
      object.remove("prop3");
    } catch (SException e) {
      assertTrue("SException expected, can't write", true);
    }

    //  field read only, restricted user list (current user NOT in list)

    nac = new SNodeAccessControl(CollectionUtils.immutableSet("bob@acme.com","ann@acme.com") ,Collections.<String>emptySet(), true);
    p = new SPrimitive("read only with access list", nac);
    object.put("prop4", p);

    try {
      object.pick("prop4");
    } catch (Exception e) {
      assertTrue("SException expected, can't read", true);
    }

    try {
      object.put("prop4", p);
    } catch (SException e) {
      assertTrue("SException expected, can't write", true);
    }

    try {
      object.remove("prop4");
    } catch (SException e) {
      assertTrue("SException expected, can't write", true);
    }

    // field with write and read access lists (current user can't write)

    nac = new SNodeAccessControl(
        CollectionUtils.immutableSet("tom@acme.com","ann@acme.com"),
        CollectionUtils.immutableSet("bob@acme.com","ann@acme.com"),
        false);

    p = new SPrimitive("read and write access list", nac);
    object.put("prop5", p);

    try {
      object.pick("prop5");
    } catch (Exception e) {
      assertTrue("SException not expected", false);
    }

    try {
      object.put("prop5", p);
    } catch (SException e) {
      assertTrue("SException expected, can't write", true);
    }

    try {
      object.remove("prop5");
    } catch (SException e) {
      assertTrue("SException expected, can't write", true);
    }
  }


  protected void tearDown() throws Exception {
    super.tearDown();
  }

}