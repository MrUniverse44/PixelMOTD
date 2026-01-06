package me.blueslime.pixelmotd.status;

/**
 * Verify the status of backends with this class, so we can show backend status in the MoTD.
 */
public interface StatusChecker {

    /**
     * Returns the localized status string for a server name.
     *
     * @param server server name
     * @return online or offline status string
     */
    String getServerStatus(String server);

}
