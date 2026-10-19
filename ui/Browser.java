package ui;

import java.awt.Component;
import java.awt.Desktop;
import java.net.URI;

/** Opens prerequisite videos in the computer's web browser. */
public final class Browser {

    private Browser() {
    }

    /** @return true if the browser was opened */
    public static boolean open(Component owner, String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception e) {
            // fall through to the manual message below
        }
        Dialogs.info(owner, "Open video", "Could not open the browser automatically.\nOpen this link yourself:\n" + url);
        return true;
    }
}
