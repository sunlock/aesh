package org.jboss.aesh.console;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandNotFoundException extends Exception {

    public CommandNotFoundException(String s) {
        super(s);
    }
}
