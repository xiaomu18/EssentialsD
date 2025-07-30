package cn.lunadeer.minecraftpluginutils.stui;

import cn.lunadeer.minecraftpluginutils.stui.components.Line;
import cn.lunadeer.minecraftpluginutils.stui.components.Pagination;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;

public class ListView {
   private final Integer page_size;
   private final List<Line> lines = new ArrayList<>();
   private String command = "";
   private final View view = View.create();

   private ListView(int page_size, String command) {
      this.page_size = page_size;
      this.command = command;
   }

   public static ListView create(int page_size, String command) {
      return new ListView(page_size, command);
   }

   public ListView title(String title) {
      this.view.title(title);
      return this;
   }

   public ListView title(String title, String subtitle) {
      this.view.title(title);
      this.view.subtitle(subtitle);
      return this;
   }

   public ListView subtitle(String subtitle) {
      this.view.subtitle(subtitle);
      return this;
   }

   public ListView subtitle(TextComponent subtitle) {
      this.view.subtitle(subtitle);
      return this;
   }

   public ListView subtitle(Line line) {
      this.view.subtitle(line);
      return this;
   }

   public ListView add(Line line) {
      this.lines.add(line);
      return this;
   }

   public ListView addLines(List lines) {
      this.lines.addAll(lines);
      return this;
   }

   public void showOn(Player player, Integer page) {
      int offset = (page - 1) * this.page_size;
      if (this.lines.isEmpty()) {
         this.lines.add(Line.create().append("啊哦，这里还没有内容～"));
      }

      if (offset > this.lines.size() || offset < 0) {
         this.view.addLine(Line.create().append(Component.text("页码错误", ViewStyles.error_color)));
         offset = 0;
         page = 1;
      }

      for(int i = offset; i < offset + this.page_size; ++i) {
         if (i >= this.lines.size()) {
            for(int j = 0; j < this.page_size - this.lines.size() % this.page_size; ++j) {
               this.view.addLine(Line.create());
            }
            break;
         }

         this.view.addLine((Line)this.lines.get(i));
      }

      this.view.actionBar(Pagination.create(page, this.lines.size(), this.page_size, this.command));
      this.view.showOn(player);
   }

   public ListView navigator(Line line) {
      Line nav = Line.create().setDivider("->").append(Component.text("导航", ViewStyles.main_color));

      for(Component component : line.getElements()) {
         nav.append(component);
      }

      this.view.subtitle(nav);
      return this;
   }
}
