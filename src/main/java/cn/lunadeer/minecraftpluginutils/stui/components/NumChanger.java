package cn.lunadeer.minecraftpluginutils.stui.components;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class NumChanger {
   private final Double value;
   private final String changeCommand;
   private Integer pageNumber;
   private Double step;

   private NumChanger(Double value, String changeCommand) {
      this.value = value;
      this.changeCommand = changeCommand;
      this.step = (double)1.0F;
   }

   private NumChanger(Double value, String changeCommand, Double step) {
      this.value = value;
      this.changeCommand = changeCommand;
      this.step = step;
   }

   public NumChanger setPageNumber(Integer pageNumber) {
      this.pageNumber = pageNumber;
      return this;
   }

   public static NumChanger create(Double value, String changeCommand) {
      return new NumChanger(value, changeCommand);
   }

   public static NumChanger create(Double value, String changeCommand, Double step) {
      return new NumChanger(value, changeCommand, step);
   }

   public static NumChanger create(Float value, String changeCommand) {
      return new NumChanger(value.doubleValue(), changeCommand);
   }

   public static NumChanger create(Float value, String changeCommand, Double step) {
      return new NumChanger(value.doubleValue(), changeCommand, step);
   }

   public static NumChanger create(Integer value, String changeCommand) {
      return new NumChanger(value.doubleValue(), changeCommand);
   }

   public static NumChanger create(Integer value, String changeCommand, Double step) {
      return new NumChanger(value.doubleValue(), changeCommand, step);
   }

   private static String intIfNoDecimal(Double value) {
      return value % (double)1.0F == (double)0.0F ? String.valueOf(value.intValue()) : String.valueOf(value);
   }

   public TextComponent build() {
      Button var10000 = Button.create("+").setPreSufIx("", "");
      String var10001 = this.changeCommand;
      Button plus = var10000.setExecuteCommand(var10001 + " " + intIfNoDecimal(this.value + this.step) + (this.pageNumber == null ? "" : " " + this.pageNumber)).setHoverText("增加" + intIfNoDecimal(this.step));
      var10000 = Button.create(">>").setPreSufIx("", "");
      var10001 = this.changeCommand;
      var10000 = var10000.setExecuteCommand(var10001 + " " + intIfNoDecimal(this.value + this.step * (double)10.0F) + (this.pageNumber == null ? "" : " " + this.pageNumber));
      double var11 = this.step;
      Button plus10 = var10000.setHoverText("增加" + intIfNoDecimal(var11 * (double)10.0F));
      var10000 = Button.create("-").setPreSufIx("", "");
      String var12 = this.changeCommand;
      Button minus = var10000.setExecuteCommand(var12 + " " + intIfNoDecimal(this.value - this.step) + (this.pageNumber == null ? "" : " " + this.pageNumber)).setHoverText("减少" + intIfNoDecimal(this.step));
      var10000 = Button.create("<<").setPreSufIx("", "");
      var12 = this.changeCommand;
      var10000 = var10000.setExecuteCommand(var12 + " " + intIfNoDecimal(this.value - this.step * (double)10.0F) + (this.pageNumber == null ? "" : " " + this.pageNumber));
      double var14 = this.step;
      Button minus10 = var10000.setHoverText("减少" + intIfNoDecimal(var14 * (double)10.0F));
      return (TextComponent)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append(minus10.build())).append(minus.build())).append(Component.text(" "))).append(Component.text(intIfNoDecimal(this.value)))).append(Component.text(" "))).append(plus.build())).append(plus10.build())).build();
   }
}
