package cn.lunadeer.minecraftpluginutils.stui.components;

import cn.lunadeer.minecraftpluginutils.stui.ViewStyles;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class Pagination {
   public static TextComponent create(int page, int item_size, int page_size, String command) {
      int page_count = (int)Math.ceil((double)item_size / (double)page_size);
      if (page_count == 0) {
         page_count = 1;
      }

      List<Component> componentList = new ArrayList();
      componentList.add(Component.text("第 ", ViewStyles.main_color));
      componentList.add(Component.text(page, ViewStyles.sub_color));
      componentList.add(Component.text("/", ViewStyles.main_color));
      componentList.add(Component.text(page_count, ViewStyles.sub_color));
      componentList.add(Component.text(" 页 ", ViewStyles.main_color));
      if (page > 1) {
         componentList.add(Button.create("上一页").setExecuteCommand(command + " " + (page - 1)).build());
      } else {
         componentList.add(Button.create("上一页").setColor(ViewStyles.sub_color).build());
      }

      if (page < page_count) {
         componentList.add(Button.create("下一页").setExecuteCommand(command + " " + (page + 1)).build());
      } else {
         componentList.add(Button.create("下一页").setColor(ViewStyles.sub_color).build());
      }

      TextComponent.Builder builder = Component.text();

      for(Component component : componentList) {
         builder.append(component);
      }

      return (TextComponent)builder.build();
   }
}
