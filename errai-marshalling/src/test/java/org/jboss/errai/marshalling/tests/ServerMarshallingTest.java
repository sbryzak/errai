package org.jboss.errai.marshalling.tests;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.Assert;

import org.jboss.errai.marshalling.client.MarshallingSessionProviderFactory;
import org.jboss.errai.marshalling.client.api.Marshaller;
import org.jboss.errai.marshalling.client.api.MarshallingSession;
import org.jboss.errai.marshalling.client.api.ParserFactory;
import org.jboss.errai.marshalling.client.api.json.EJValue;
import org.jboss.errai.marshalling.server.MappingContextSingleton;
import org.jboss.errai.marshalling.server.ServerMarshalling;
import org.jboss.errai.marshalling.tests.res.SType;
import org.jboss.errai.marshalling.tests.res.shared.Role;
import org.jboss.errai.marshalling.tests.res.shared.User;
import org.junit.Test;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class ServerMarshallingTest {

  static {
    System.setProperty("errai.devel.nocache", "true");
  }

  @SuppressWarnings("unchecked")
  private void testEncodeDecodeDynamic(Object value) {
    if (value == null) return;
    testEncodeDecode((Class<Object>) value.getClass(), value);
  }

  private <T> void testEncodeDecode(Class<T> type, T value) {
    Marshaller<Object> marshaller = MappingContextSingleton.get().getMarshaller(type.getName());
    Assert.assertNotNull("did not find " + type.getName() + " marshaller", marshaller);

    MarshallingSession encSession = MarshallingSessionProviderFactory.getEncoding();
    String enc = "[" + marshaller.marshall(value, encSession) + "]";

    System.out.println("encoded: " + enc);

    MarshallingSession decSession = MarshallingSessionProviderFactory.getDecoding();
    EJValue parsedJson = ParserFactory.get().parse(enc);
    Assert.assertTrue("expected outer JSON to be array", parsedJson.isArray() != null);

    EJValue encodedNode = parsedJson.isArray().get(0);

    Object dec = marshaller.demarshall(encodedNode, decSession);
    Assert.assertTrue("decoded type not an instance of String", type.isAssignableFrom(value.getClass()));
    assertEquals(value, dec);
  }

  private static void assertEquals(Object a1, Object a2) {
    if (a1 != null && a2 != null) {
      if (a1.getClass().isArray()) {
        if (a2.getClass().isArray()) {
          assertArrayEquals(a1, a2);
          return;
        }

      }
    }

    Assert.assertEquals(a1, a2);
  }

  private static void assertArrayEquals(Object array1, Object array2) {
    int len1 = Array.getLength(array1);
    int len2 = Array.getLength(array2);

    if (len1 != len2) Assert.failNotEquals("different length arrays!", array1, array2);

    Object el1, el2;

    for (int i = 0; i < len1; i++) {
      el1 = Array.get(array1, i);
      el2 = Array.get(array2, i);

      if ((el1 == null || el2 == null) && el1 != null) {
        Assert.failNotEquals("different values", array1, array2);
      }
      else if (el1 != null) {
        assertEquals(el1, el2);
      }
    }
  }

  @Test
  public void testString() {
    testEncodeDecode(String.class, "ThisIsOurTestString");
  }

  @Test
  public void testEscapesInString() {
    testEncodeDecode(String.class, "\n\t\r\n{}{}{}\\}\\{\\]\\[");
  }

  @Test
  public void testStringArray() {
    testEncodeDecode(String[].class, new String[]{"foo", "bar", "superfoobar", "ultrafoobar"});
  }

  @Test
  public void testIntegerArray() {
    testEncodeDecode(Integer[].class, new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
  }

  @Test
  public void testPrimIntegerArray() {
    testEncodeDecode(int[].class, new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
  }

  @Test
  public void testLongArray() {
    testEncodeDecode(Long[].class, new Long[]{1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l, 9l});
  }

  @Test
  public void testPrimLongArray() {
    testEncodeDecode(long[].class, new long[]{1l, 2l, 3l, 4l, 5l, 6l, 7l, 8l, 9l});
  }


  @Test
  public void testIntegerMaxValue() {
    testEncodeDecode(Integer.class, Integer.MAX_VALUE);
  }

  @Test
  public void testIntegerMinValue() {
    testEncodeDecode(Integer.class, Integer.MIN_VALUE);
  }

  @Test
  public void testIntegerRandomValue() {
    testEncodeDecode(Integer.class, new Random(System.currentTimeMillis()).nextInt(Integer.MAX_VALUE));
  }

  @Test
  public void testShortMinValue() {
    testEncodeDecode(Short.class, Short.MAX_VALUE);
  }

  @Test
  public void testShortMaxValue() {
    testEncodeDecode(Short.class, Short.MIN_VALUE);
  }

  @Test
  public void testShortRandomValue() {
    testEncodeDecode(Short.class, (short) new Random(System.currentTimeMillis()).nextInt(Short.MAX_VALUE));
  }

  @Test
  public void testLongMaxValue() {
    testEncodeDecode(Long.class, Long.MAX_VALUE);
  }

  @Test
  public void testLongMinValue() {
    testEncodeDecode(Long.class, Long.MIN_VALUE);
  }

  @Test
  public void testLong6536000376648360988() {
    testEncodeDecode(Long.class, 6536000376648360988L);
  }

  @Test
  public void testLongRandomValue() {
    testEncodeDecode(Long.class, new Random(System.currentTimeMillis()).nextLong());
  }

  @Test
  public void testDoubleMaxValue() {
    testEncodeDecode(Double.class, Double.MAX_VALUE);
  }

  @Test
  public void testDoubleMinValue() {
    testEncodeDecode(Double.class, Double.MIN_VALUE);
  }

  @Test
  public void testDoubleRandomValue() {
    testEncodeDecode(Double.class, new Random(System.currentTimeMillis()).nextDouble());
  }

  @Test
  public void testDouble0dot9635950160419999() {
    testEncodeDecode(Double.class, 0.9635950160419999d);
  }

  @Test
  public void testFloatMaxValue() {
    testEncodeDecode(Float.class, Float.MAX_VALUE);
  }

  @Test
  public void testFloatMinValue() {
    testEncodeDecode(Float.class, Float.MIN_VALUE);
  }

  @Test
  public void testFloatRandomValue() {
    testEncodeDecode(Float.class, new Random(System.currentTimeMillis()).nextFloat());
  }

  @Test
  public void testByteMaxValue() {
    testEncodeDecode(Byte.class, Byte.MAX_VALUE);
  }

  @Test
  public void testByteMinValue() {
    testEncodeDecode(Byte.class, Byte.MIN_VALUE);
  }

  @Test
  public void testByteRandomValue() {
    testEncodeDecode(Byte.class, (byte) new Random(System.currentTimeMillis()).nextInt());
  }

  @Test
  public void testBooleanTrue() {
    testEncodeDecode(Boolean.class, Boolean.TRUE);
  }

  @Test
  public void testBooleanFalse() {
    testEncodeDecode(Boolean.class, Boolean.FALSE);
  }

  @Test
  public void testCharMaxValue() {
    testEncodeDecode(Character.class, Character.MAX_VALUE);
  }

  @Test
  public void testCharMinValue() {
    testEncodeDecode(Character.class, Character.MIN_VALUE);
  }

  @Test
  public void testListMarshall() {
    testEncodeDecodeDynamic(Arrays.asList("foo", "bar", "sillyhat"));
  }

  @Test
  public void testUnmodifiableListMarshall() {
    testEncodeDecodeDynamic(Collections.unmodifiableList(Arrays.asList("foo", "bar", "sillyhat")));
  }

  @Test
  public void testUnmodifiableSetMarshall() {
    testEncodeDecodeDynamic(Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("foo", "bar", "sillyhat"))));
  }

  @Test
  public void testUnmodifiableSortedSetMarshall() {
    testEncodeDecodeDynamic(Collections.unmodifiableSortedSet(new TreeSet<String>(Arrays.asList("foo", "bar", "sillyhat"))));
  }

  @Test
  public void testSingletonListMarshall() {
    testEncodeDecodeDynamic(Collections.singletonList("foobie"));
  }

  @Test
  public void testSetMarshall() {
    testEncodeDecodeDynamic(new HashSet<String>(Arrays.asList("foo", "bar", "sillyhat")));
  }

  @Test
  public void testEmptyList() {
    testEncodeDecodeDynamic(Collections.emptyList());
  }

  @Test
  public void testEmptySet() {
    testEncodeDecodeDynamic(Collections.emptySet());
  }

  @Test
  public void testEmptyMap() {
    testEncodeDecodeDynamic(Collections.emptyMap());
  }

  @Test
  public void testSynchronizedSortedMap() {
    TreeMap<String, String> map = new TreeMap<String, String>();
    map.put("a", "a");
    map.put("b", "b");
    map.put("c", "c");
    testEncodeDecodeDynamic(Collections.synchronizedSortedMap(map));
  }

  @Test
  public void testSynchronizedMap() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("a", "a");
    map.put("b", "b");
    map.put("c", "c");
    testEncodeDecodeDynamic(Collections.synchronizedMap(map));
  }

  @Test
  public void testSynchronizedSortedSet() {
    TreeSet<String> set = new TreeSet<String>();
    set.add("a");
    set.add("b");
    set.add("c");
    testEncodeDecodeDynamic(Collections.synchronizedSortedSet(set));
  }

  @Test
  public void testSynchronizedSet() {
    HashSet<String> set = new HashSet<String>();
    set.add("a");
    set.add("b");
    set.add("c");
    testEncodeDecodeDynamic(Collections.synchronizedSet(set));
  }

  @Test
  public void testUserEntity() {
    User user = new User();
    user.setUserName("foo");
    user.setPassword("bar");

    Set<Role> roles = new HashSet<Role>();
    roles.add(new Role("admin"));
    roles.add(new Role("users"));

    user.setRoles(roles);

    testEncodeDecodeDynamic(user);
  }

  class ServerRandomProvider implements RandomProvider {
    private char[] CHARS = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};

    private Random random = new Random(System.nanoTime());

    public boolean nextBoolean() {
      return random.nextBoolean();
    }

    public int nextInt(int upper) {
      return random.nextInt(upper);
    }

    public double nextDouble() {
      return new BigDecimal(random.nextDouble(), MathContext.DECIMAL32).doubleValue();
    }

    public char nextChar() {
      return CHARS[nextInt(1000) % CHARS.length];
    }

    public String randString() {
      StringBuilder builder = new StringBuilder();
      int len = nextInt(25) + 5;
      for (int i = 0; i < len; i++) {
        builder.append(nextChar());
      }
      return builder.toString();
    }
  }

  public interface RandomProvider {
    public boolean nextBoolean();

    public int nextInt(int upper);

    public double nextDouble();

    public char nextChar();

    public String randString();
  }

  @Test
  public void testSTypeEntity() {
    SType sType = SType.create(new ServerRandomProvider());

//    long st = System.currentTimeMillis();
//    for (int i = 0; i < 10000; i++) {
    testEncodeDecodeDynamic(sType);
    //  }
//    System.out.println(System.currentTimeMillis() - st);
  }

  @Test
  public void testPrimitiveIntRoundTrip() {
    final int val = 1701;
    String json = ServerMarshalling.toJSON(val);
    Assert.assertEquals("Failed to marshall/demarshall int", val, ServerMarshalling.fromJSON(json));
  }

  @Test
  public void testPrimitiveLongRoundTrip() {
    final long val = 1701l;
    String json = ServerMarshalling.toJSON(val);
    Assert.assertEquals("Failed to marshall/demarshall long", val, ServerMarshalling.fromJSON(json));
  }

  @Test
  public void testPrimitiveDoubleRoundTrip() {
    final double val = 17.01;
    String json = ServerMarshalling.toJSON(val);
    Assert.assertEquals("Failed to marshall/demarshall double", val, ServerMarshalling.fromJSON(json));
  }

  @Test
  public void testPrimitiveFloatRoundTrip() {
    final float val = 1701f;
    String json = ServerMarshalling.toJSON(val);
    Assert.assertEquals("Failed to marshall/demarshall float", val, ServerMarshalling.fromJSON(json));
  }
}
