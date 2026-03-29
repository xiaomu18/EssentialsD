package cn.lunadeer.utils.stui.components.buttons;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;

public abstract class ListViewButton extends PermissionButton {

    public abstract String getCommand(String page);

    public ListViewButton(String text) {
        super(text);
    }

    public TextComponent build(String page) {
        this.action = ClickEvent.Action.RUN_COMMAND;
        this.clickExecute = getCommand(page);
        return super.build();
    }

    public TextComponent build(String page, String text) {
        this.text = text;
        return build(page);
    }
}
