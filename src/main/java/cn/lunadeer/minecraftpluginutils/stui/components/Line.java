package cn.lunadeer.minecraftpluginutils.stui.components;

import cn.lunadeer.minecraftpluginutils.stui.ViewStyles;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class Line {
   private String d = " - ";
   private final List<Component> elements = new ArrayList<>();

   public TextComponent build() {
      TextComponent divider = Component.text(this.d, ViewStyles.sub_color);
      TextComponent.Builder builder = Component.text();

      for(int i = 0; i < this.elements.size(); ++i) {
         builder.append((Component)this.elements.get(i));
         if (i != this.elements.size() - 1) {
            builder.append(divider);
         }
      }

      return (TextComponent)builder.build();
   }

   public static Line create() {
      return new Line();
   }

   public List<Component> getElements() {
      return this.elements;
   }

   public Line append(TextComponent component) {
      this.elements.add(component);
      return this;
   }

   public Line setDivider(String d) {
      this.d = d;
      return this;
   }

   public Line append(Component component) {
      this.elements.add(component);
      return this;
   }

   public Line append(String component) {
      this.elements.add(Component.text(component));
      return this;
   }
}
