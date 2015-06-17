package org.jboss.pnc.buildagent;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TestProcess {

  public static final String WELCOME_MESSAGE = "Hi there! I'm a long running process.";
  public static final String MESSAGE = "Hello again!";

  public static void main(String[] args) throws InterruptedException {

    int delay = 250;
    int repeat = 40;

    if (args.length >= 1) {
      repeat = Integer.parseInt(args[0]);
    }

    if (args.length >= 2) {
      delay = Integer.parseInt(args[1]);
    }

    System.out.println(WELCOME_MESSAGE);
    System.out.println("I'll write to stdout test message '" + MESSAGE + "' " + repeat + " times with " + delay + "ms delay.");
    for (int i = 0; i < repeat; i++) {
      System.out.println(i + " : " + MESSAGE);
      Thread.sleep(delay);
    }
  }
}
