package cn.lunadeer.minecraftpluginutils.stui;

import cn.lunadeer.minecraftpluginutils.Notification;
import cn.lunadeer.minecraftpluginutils.stui.components.Line;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class View {
   protected TextComponent space = Component.text(" ");
   protected TextComponent sub_title_decorate;
   protected TextComponent line_decorate;
   protected TextComponent action_decorate;
   protected TextComponent title;
   protected TextComponent subtitle;
   protected List<TextComponent> content_lines;
   protected TextComponent actionbar;
   protected TextComponent edge;
   protected TextComponent divide_line;

   public View() {
      this.sub_title_decorate = Component.text("-   ", ViewStyles.main_color);
      this.line_decorate = Component.text("⌗ ", ViewStyles.main_color);
      this.action_decorate = Component.text("▸ ", ViewStyles.main_color);
      this.title = Component.text("       ");
      this.subtitle = null;
      this.content_lines = new ArrayList<>();
      this.actionbar = null;
      this.edge = Component.text("                                                               ", Style.style(ViewStyles.main_color, new TextDecoration[]{TextDecoration.STRIKETHROUGH}));
      this.divide_line = Component.text("                                                               ", Style.style(ViewStyles.main_color, new TextDecoration[]{TextDecoration.STRIKETHROUGH}));
   }

   public void showOn(Player player) {
      TextComponent.Builder builder = Component.text();
      builder.append(Component.text("__/", ViewStyles.main_color));
      ((TextComponent.Builder)((TextComponent.Builder)builder.append(this.space)).append(this.title)).append(this.space);
      builder.append(Component.text("\\__", ViewStyles.main_color));
      Notification.instance.sender.sendMessage((Player)player, builder.build());
      if (this.subtitle != null) {
         Notification.instance.sender.sendMessage((Player)player, this.divide_line);
         Notification.instance.sender.sendMessage((Player)player, ((TextComponent.Builder)((TextComponent.Builder)Component.text().append(this.sub_title_decorate)).append(this.subtitle)).build());
      }

      Notification.instance.sender.sendMessage((Player)player, this.divide_line);

      for(TextComponent content_line : this.content_lines) {
         Notification.instance.sender.sendMessage((Player)player, ((TextComponent.Builder)((TextComponent.Builder)Component.text().append(this.line_decorate)).append(content_line)).build());
      }

      if (this.actionbar != null) {
         Notification.instance.sender.sendMessage((Player)player, this.divide_line);
         Notification.instance.sender.sendMessage((Player)player, ((TextComponent.Builder)((TextComponent.Builder)Component.text().append(this.action_decorate)).append(this.actionbar)).build());
      }

      Notification.instance.sender.sendMessage((Player)player, this.edge);
      Notification.instance.sender.sendMessage((Player)player, Component.text("     "));
   }

   public static View create() {
      return new View();
   }

   public View title(String title) {
      this.title = Component.text(title);
      return this;
   }

   public View title(TextComponent title) {
      this.title = title;
      return this;
   }

   public View subtitle(String subtitle) {
      this.subtitle = Component.text(subtitle);
      return this;
   }

   public View subtitle(Line line) {
      this.subtitle = line.build();
      return this;
   }

   public View navigator(Line line) {
      Line nav = Line.create().setDivider("->").append(Component.text("导航", ViewStyles.main_color));

      for(Component component : line.getElements()) {
         nav.append(component);
      }

      return this.subtitle(nav);
   }

   public View subtitle(TextComponent subtitle) {
      this.subtitle = subtitle;
      return this;
   }

   public View actionBar(TextComponent actionbar) {
      this.actionbar = actionbar;
      return this;
   }

   public View actionBar(String actionbar) {
      this.actionbar = Component.text(actionbar);
      return this;
   }

   public View actionBar(Line actionbar) {
      this.actionbar = actionbar.build();
      return this;
   }

   public View addLine(TextComponent component) {
      this.content_lines.add(component);
      return this;
   }

   public View addLine(String component) {
      this.content_lines.add(Component.text(component));
      return this;
   }

   public View addLine(Line component) {
      this.content_lines.add(component.build());
      return this;
   }
}
