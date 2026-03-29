package cn.lunadeer.utils.stui.inputter;

import cn.lunadeer.utils.Notification;
import cn.lunadeer.utils.XLogger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class InputterRunner {

    public static String ONLY_PLAYER = "TUI inputter can only be used by a player.";
    public static String CANCEL = " [Send 'C' to cancel the inputter.]";
    public static String INPUTTER_CANCELLED = "Inputter cancelled.";

    private Player sender;

    public InputterRunner(CommandSender sender, String hint) {
        if (!(sender instanceof Player player)) {
            Notification.error(sender, ONLY_PLAYER);
            return;
        }
        this.sender = player;
        Inputter.getInstance().register(this);
        Notification.info(sender, hint + CANCEL);
    }

    public void runner(String inputter) {
        Inputter.getInstance().unregister(this);
        try {
            if (inputter.equalsIgnoreCase("C")) {
                Notification.warn(sender, INPUTTER_CANCELLED);
                cancelRun();
            } else {
                run(inputter);
            }
        } catch (Exception e) {
            Notification.error(sender, e.getMessage());
            XLogger.error(e);
        }
    }

    public abstract void run(String inputter);

    public void cancelRun() {
    }

    public Player getSender() {
        return sender;
    }
}
