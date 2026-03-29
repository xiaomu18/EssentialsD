package cn.lunadeer.utils.stui.components;

import cn.lunadeer.utils.stui.components.buttons.Button;
import cn.lunadeer.utils.stui.components.buttons.ListViewButton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

import static cn.lunadeer.utils.stui.ViewStyles.PRIMARY;
import static cn.lunadeer.utils.stui.ViewStyles.SECONDARY;

public class Pagination {
    public static TextComponent create(int page, int itemSize, int pageSize, ListViewButton command) {
        int pageCount = (int) Math.ceil((double) itemSize / pageSize);
        if (pageCount == 0) {
            pageCount = 1;
        }

        List<Component> componentList = new ArrayList<>();
        componentList.add(Component.text("[", PRIMARY));
        componentList.add(Component.text(page, SECONDARY));
        componentList.add(Component.text("/", PRIMARY));
        componentList.add(Component.text(pageCount, SECONDARY));
        componentList.add(Component.text("]", PRIMARY));

        if (page > 1) {
            componentList.add(command.build(String.valueOf(page - 1), "<"));
        } else {
            componentList.add(new Button("<").setColor(SECONDARY).build());
        }
        if (page < pageCount) {
            componentList.add(command.build(String.valueOf(page + 1), ">"));
        } else {
            componentList.add(new Button(">").setColor(SECONDARY).build());
        }

        TextComponent.Builder builder = Component.text();
        for (Component component : componentList) {
            builder.append(component);
        }
        return builder.build();
    }
}
