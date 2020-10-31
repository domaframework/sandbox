package org.seasar.doma.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EchoTest {
  @Test
  void test() {
    Echo echo = new Echo();
    assertEquals("hello", echo.echo("hello"));
  }
}
