package cn.lunadeer.minecraftpluginutils.stui.components;

import cn.lunadeer.minecraftpluginutils.stui.ViewStyles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.ClickEvent.Action;
import net.kyori.adventure.text.format.TextColor;

public class Button {
   private String prefix = "[";
   private String suffix = "]";
   private String text = "";
   private ClickEvent.Action action = null;
   private String hoverText = "";
   private String clickExecute = "";
   TextColor color;

   public Button() {
      this.color = ViewStyles.action_color;
   }

   public TextComponent build() {
      TextComponent.Builder builder = Component.text();
      builder.append(Component.text(this.prefix, this.color));
      builder.append(Component.text(this.text, this.color));
      builder.append(Component.text(this.suffix, this.color));
      if (this.action != null) {
         builder.clickEvent(ClickEvent.clickEvent(this.action, this.clickExecute));
      }

      if (!this.hoverText.isEmpty()) {
         builder.hoverEvent(Component.text(this.hoverText));
      }

      return (TextComponent)builder.build();
   }

   public Button setPreSufIx(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
      return this;
   }

   public Button setExecuteCommand(String command) {
      this.action = Action.RUN_COMMAND;
      this.clickExecute = command;
      return this;
   }

   public Button setOpenURL(String url) {
      this.action = Action.OPEN_URL;
      this.clickExecute = url;
      return this;
   }

   public Button setCopyToClipboard() {
      this.action = Action.COPY_TO_CLIPBOARD;
      this.clickExecute = this.text;
      return this;
   }

   public Button setHoverText(String hoverText) {
      this.hoverText = hoverText;
      return this;
   }

   public Button setColor(TextColor color) {
      this.color = color;
      return this;
   }

   public static Button create(String text) {
      Button btn = new Button();
      btn.text = text;
      return btn;
   }

   public static Button createRed(String text) {
      Button btn = new Button();
      btn.text = text;
      btn.color = ViewStyles.error_color;
      return btn;
   }

   public static Button createGreen(String text) {
      Button btn = new Button();
      btn.text = text;
      btn.color = ViewStyles.success_color;
      return btn;
   }
}
