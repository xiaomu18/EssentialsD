package cn.lunadeer.utils.stui.components.buttons;

import cn.lunadeer.utils.command.CommandManager;
import cn.lunadeer.utils.command.NoPermissionException;
import cn.lunadeer.utils.command.SecondaryCommand;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public abstract class FunctionalButton extends PermissionButton {

    public abstract void function();


    public FunctionalButton(String text) {
        super(text);
        UUID uuid = UUID.randomUUID();
        new SecondaryCommand("tui_btn_future_" + uuid) {
            @Override
            public void executeHandler(CommandSender sender) {
                for (String permission : permissions) {
                    if (!sender.hasPermission(permission))
                        throw new NoPermissionException(permission);
                }
                function();
            }
        }.dynamic().register();
        this.action = ClickEvent.Action.RUN_COMMAND;
        this.clickExecute = CommandManager.getRootCommand() + " tui_btn_future_" + uuid;
    }

}
