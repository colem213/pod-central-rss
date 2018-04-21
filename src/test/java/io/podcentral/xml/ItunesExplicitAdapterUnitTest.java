package io.podcentral.xml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ItunesExplicitAdapterUnitTest {
  @Test
  public void yesIsExplicit() throws Exception {
    assertTrue(new ItunesExplicitAdapter().unmarshal("yes"));
  }

  @Test
  public void explicitIsExplicit() throws Exception {
    assertTrue(new ItunesExplicitAdapter().unmarshal("explicit"));
  }

  @Test
  public void trueIsExplicit() throws Exception {
    assertTrue(new ItunesExplicitAdapter().unmarshal("true"));
  }

  @Test
  public void noIsNotExplicit() throws Exception {
    assertFalse(new ItunesExplicitAdapter().unmarshal("no"));
  }

  @Test
  public void cleanIsNotExplicit() throws Exception {
    assertFalse(new ItunesExplicitAdapter().unmarshal("clean"));
  }

  @Test
  public void falseIsNotExplicit() throws Exception {
    assertFalse(new ItunesExplicitAdapter().unmarshal("false"));
  }
}
